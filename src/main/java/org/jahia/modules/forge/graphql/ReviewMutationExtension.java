package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.usermanager.JahiaUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.Locale;

/**
 * GraphQL mutation that lets an authenticated user submit a review on any forge
 * module/package.
 *
 * <p>Unlike the generic {@code jcr} mutations the storefront uses for owner-gated
 * authoring, reviewing is intentionally <em>cross-owner</em>: any logged-in user
 * may review a module they do not own and therefore have no {@code jcr:write} on.
 * The legacy {@code AddReview}/{@code rate} Spring-MVC action chain handled this
 * server-side; this mutation is its JavaScript-module replacement.
 *
 * <p>It runs under a system session impersonating the caller — which bypasses the
 * module ACL (so the write succeeds) while keeping {@code jcr:createdBy} set to
 * the real user (so the review is correctly attributed and the one-review-per-user
 * guard works). The caller is still required to be authenticated; guests are
 * rejected.
 */
@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLName("ForgeReviewMutations")
@GraphQLDescription("Private App Store module review mutations")
public final class ReviewMutationExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewMutationExtension.class);

    private static final String WORKSPACE_DEFAULT = "default";
    private static final String JMIX_REVIEWS = "jmix:reviews";
    private static final String JMIX_RATING = "jmix:rating";
    private static final String JMIX_FORGE_ELEMENT = "jmix:forgeElement";
    private static final String JNT_REVIEW = "jnt:review";
    private static final String JNT_REVIEWS = "jnt:reviews";
    private static final String REVIEWS_CHILD = "reviews";
    private static final String PROP_RATING = "rating";
    private static final String PROP_CONTENT = "content";
    private static final String PROP_JCR_TITLE = "jcr:title";
    private static final String PROP_JCR_CREATED_BY = "jcr:createdBy";
    private static final String PROP_SUM_OF_VOTES = "j:sumOfVotes";
    private static final String PROP_NB_OF_VOTES = "j:nbOfVotes";
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    private ReviewMutationExtension() {
    }

    @GraphQLField
    @GraphQLName("submitForgeReview")
    @GraphQLDescription("Submit a review (rating 1-5 plus optional title and comment) on a forge module or "
            + "package. Requires an authenticated user; one review per user per module.")
    public static GqlForgeReview submitForgeReview(
            @GraphQLName("moduleId") @GraphQLNonNull
            @GraphQLDescription("UUID of the forge module/package node") final String moduleId,
            @GraphQLName("rating") @GraphQLNonNull
            @GraphQLDescription("Rating from 1 to 5") final int rating,
            @GraphQLName("title")
            @GraphQLDescription("Optional review title") final String title,
            @GraphQLName("comment")
            @GraphQLDescription("Optional review body (stored as plain text)") final String comment,
            @GraphQLName("language")
            @GraphQLDescription("Content language tag, e.g. \"en\" (defaults to English)") final String language) {

        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new ForgeReviewException("Rating must be between " + MIN_RATING + " and " + MAX_RATING);
        }

        final JahiaUser user = JCRSessionFactory.getInstance().getCurrentUser();
        if (user == null || user.getName() == null || Constants.GUEST_USERNAME.equals(user.getName())) {
            throw new ForgeReviewException("You must be logged in to submit a review");
        }
        final String username = user.getName();
        final Locale locale = StringUtils.isBlank(language)
                ? Locale.ENGLISH : Locale.forLanguageTag(language);

        try {
            // System session impersonating the caller: ACL-bypassing write (cross-owner
            // reviews) while jcr:createdBy is stamped with the real user.
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(user, WORKSPACE_DEFAULT, locale, session -> {
                final JCRNodeWrapper module;
                try {
                    module = session.getNodeByIdentifier(moduleId);
                } catch (ItemNotFoundException e) {
                    throw new ForgeReviewException("Module not found: " + moduleId);
                }
                if (!module.isNodeType(JMIX_FORGE_ELEMENT)) {
                    throw new ForgeReviewException("Node is not a reviewable forge element: " + moduleId);
                }

                final JCRNodeWrapper reviews = reviewsContainer(module);
                rejectIfAlreadyReviewed(reviews, username);

                final String nodeName = JCRContentUtils.findAvailableNodeName(reviews, "review");
                final JCRNodeWrapper review = reviews.addNode(nodeName, JNT_REVIEW);
                review.setProperty(PROP_RATING, (long) rating);
                if (StringUtils.isNotBlank(title)) {
                    review.setProperty(PROP_JCR_TITLE, title);
                }
                if (StringUtils.isNotBlank(comment)) {
                    review.setProperty(PROP_CONTENT, comment);
                }
                // Let the author manage (e.g. delete) their own review, mirroring legacy AddReview.
                review.grantRoles("u:" + username, Collections.singleton("owner"));

                final long[] aggregate = updateAggregateRating(module, rating);
                session.save();

                final long count = aggregate[1];
                final double average = count > 0 ? (double) aggregate[0] / (double) count : 0d;
                return new GqlForgeReview(moduleId, rating, title, comment, username, count, average);
            });
        } catch (RepositoryException e) {
            LOGGER.error("Failed to submit review for module {}", moduleId, e);
            throw new ForgeReviewException("Could not submit the review", e);
        }
    }

    /** The module's {@code reviews} container (autocreated by jmix:reviews; created defensively otherwise). */
    private static JCRNodeWrapper reviewsContainer(JCRNodeWrapper module) throws RepositoryException {
        if (!module.isNodeType(JMIX_REVIEWS)) {
            module.addMixin(JMIX_REVIEWS);
        }
        return module.hasNode(REVIEWS_CHILD)
                ? module.getNode(REVIEWS_CHILD)
                : module.addNode(REVIEWS_CHILD, JNT_REVIEWS);
    }

    private static void rejectIfAlreadyReviewed(JCRNodeWrapper reviews, String username) throws RepositoryException {
        final NodeIterator it = reviews.getNodes();
        while (it.hasNext()) {
            final Node n = it.nextNode();
            if (n.isNodeType(JNT_REVIEW)
                    && n.hasProperty(PROP_JCR_CREATED_BY)
                    && username.equals(n.getProperty(PROP_JCR_CREATED_BY).getString())) {
                throw new ForgeReviewException("You have already reviewed this module");
            }
        }
    }

    /**
     * Maintain the module's aggregate rating (jmix:rating), the same bookkeeping the legacy
     * {@code rate} action did. Returns {@code [sumOfVotes, nbOfVotes]} after the increment.
     */
    private static long[] updateAggregateRating(JCRNodeWrapper module, int rating) throws RepositoryException {
        if (!module.isNodeType(JMIX_RATING)) {
            module.addMixin(JMIX_RATING);
        }
        final long sum = (module.hasProperty(PROP_SUM_OF_VOTES) ? module.getProperty(PROP_SUM_OF_VOTES).getLong() : 0L)
                + rating;
        final long nb = (module.hasProperty(PROP_NB_OF_VOTES) ? module.getProperty(PROP_NB_OF_VOTES).getLong() : 0L)
                + 1L;
        module.setProperty(PROP_SUM_OF_VOTES, sum);
        module.setProperty(PROP_NB_OF_VOTES, nb);
        return new long[]{sum, nb};
    }
}
