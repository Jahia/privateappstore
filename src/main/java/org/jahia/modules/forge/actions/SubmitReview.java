/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.forge.actions;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUser;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Submit a review (rating + optional title/comment) on a forge module or package.
 *
 * <p>This is the JavaScript-module replacement for the legacy {@code rate}/{@code AddReview}
 * action chain. Reviewing is intentionally <em>cross-owner</em>: any authenticated user may
 * review a module they do not own and therefore have no {@code jcr:write} on. It is exposed as
 * a Jahia <strong>Action</strong> (not a GraphQL mutation) because the Jahia GraphQL endpoint
 * is permission-gated and not reachable by ordinary users, whereas actions are invoked through
 * the render servlet and are available to any authenticated user.
 *
 * <p>The write runs under a system session impersonating the caller
 * ({@code doExecuteWithSystemSessionAsUser}) so it bypasses the module ACL while keeping
 * {@code jcr:createdBy} set to the real user — which the review display and the
 * one-review-per-user guard both rely on. The review is created in the same workspace the page
 * is rendered in (live or default), and the module's aggregate rating (jmix:rating) is
 * maintained as the legacy {@code rate} action did.
 */
@Component(service = Action.class)
public class SubmitReview extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitReview.class);

    private static final String JMIX_REVIEWS = "jmix:reviews";
    private static final String JMIX_RATING = "jmix:rating";
    private static final String JMIX_FORGE_ELEMENT = "jmix:forgeElement";
    private static final String JNT_REVIEW = "jnt:review";
    private static final String JNT_REVIEWS = "jnt:reviews";
    private static final String REVIEWS_CHILD = "reviews";
    private static final String PROP_RATING = "rating";
    private static final String PROP_CONTENT = "content";
    private static final String PROP_JCR_TITLE = "jcr:title";
    private static final String PROP_SUM_OF_VOTES = "j:sumOfVotes";
    private static final String PROP_NB_OF_VOTES = "j:nbOfVotes";
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_COMMENT_LENGTH = 4000;

    @Activate
    public void activate() {
        // PascalCase to match the sibling actions (AddReview, CreateEntryFromJar, …);
        // the JS form posts to "<module>.SubmitReview.do".
        setName("SubmitReview");
        setRequireAuthenticatedUser(true);
        setRequiredMethods("POST");
    }

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        final int rating = parseRating(getParameter(parameters, PROP_RATING, getParameter(parameters, "j:lastVote")));
        if (rating < MIN_RATING || rating > MAX_RATING) {
            return error(HttpServletResponse.SC_BAD_REQUEST, "Rating must be between 1 and 5");
        }

        final JahiaUser user = renderContext.getUser();
        final String username = user.getUsername();
        final String title = getParameter(parameters, PROP_JCR_TITLE, getParameter(parameters, "title"));
        final String comment = getParameter(parameters, PROP_CONTENT, getParameter(parameters, "comment"));
        // The client caps these too, but a direct POST bypasses the form.
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            return error(HttpServletResponse.SC_BAD_REQUEST, "Title is too long");
        }
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            return error(HttpServletResponse.SC_BAD_REQUEST, "Comment is too long");
        }
        final String moduleId = resource.getNode().getIdentifier();
        final String workspace = session.getWorkspace().getName();
        final Locale locale = resolveLocale(getParameter(parameters, "language"), resource);

        try {
            // System session impersonating the caller, in the page's own workspace: an
            // ACL-bypassing write attributed to the real user.
            final long[] aggregate = JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(
                    user, workspace, locale, elevated -> {
                        final JCRNodeWrapper module;
                        try {
                            module = elevated.getNodeByIdentifier(moduleId);
                        } catch (ItemNotFoundException e) {
                            throw new ReviewException(HttpServletResponse.SC_NOT_FOUND, "Module not found");
                        }
                        if (!module.isNodeType(JMIX_FORGE_ELEMENT)) {
                            throw new ReviewException(HttpServletResponse.SC_BAD_REQUEST,
                                    "Not a reviewable forge element");
                        }

                        final JCRNodeWrapper reviews = reviewsContainer(module);

                        // One review per user, enforced atomically: the review node name is
                        // derived deterministically from the username, so a second review
                        // (double-submit or concurrent request) collides on the node name and
                        // is rejected — no read-then-write race window.
                        final String nodeName = "review-" + Text.escapeIllegalJcrChars(username);
                        final JCRNodeWrapper review;
                        try {
                            review = reviews.addNode(nodeName, JNT_REVIEW);
                        } catch (ItemExistsException e) {
                            throw new ReviewException(HttpServletResponse.SC_CONFLICT,
                                    "You have already reviewed this module");
                        }
                        review.setProperty(PROP_RATING, (long) rating);
                        if (StringUtils.isNotBlank(title)) {
                            review.setProperty(PROP_JCR_TITLE, title);
                        }
                        if (StringUtils.isNotBlank(comment)) {
                            review.setProperty(PROP_CONTENT, comment);
                        }
                        review.grantRoles("u:" + username, Collections.singleton("owner"));

                        final long[] agg = recomputeAggregateRating(module, reviews);
                        elevated.save();
                        return agg;
                    });

            final long count = aggregate[1];
            final double average = count > 0 ? (double) aggregate[0] / (double) count : 0d;
            final JSONObject json = new JSONObject();
            json.put("author", username);
            json.put("rating", rating);
            json.put("reviewCount", count);
            json.put("averageRating", average);
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        } catch (ReviewException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (RepositoryException e) {
            // A ReviewException is a RuntimeException and normally propagates unwrapped, but
            // unpack defensively in case the template wraps it; also map a save-time
            // same-name collision (the concurrent-request loser) to the one-per-user conflict.
            if (e.getCause() instanceof ReviewException) {
                final ReviewException re = (ReviewException) e.getCause();
                return error(re.getStatus(), re.getMessage());
            }
            if (e instanceof ItemExistsException) {
                return error(HttpServletResponse.SC_CONFLICT, "You have already reviewed this module");
            }
            LOGGER.error("Failed to submit review for module {}", moduleId, e);
            return error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not submit the review");
        }
    }

    private static int parseRating(String raw) {
        if (StringUtils.isBlank(raw)) {
            return -1;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Locale resolveLocale(String language, Resource resource) {
        if (StringUtils.isNotBlank(language)) {
            return Locale.forLanguageTag(language);
        }
        return resource.getLocale() != null ? resource.getLocale() : Locale.ENGLISH;
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

    /**
     * Recompute the module's aggregate rating (jmix:rating) from the review children, rather than
     * incrementing in place — this is self-healing under concurrent writes (an incremental
     * read-modify-write loses updates) and after review deletions. Returns
     * {@code [sumOfVotes, nbOfVotes]}. The detail page derives the displayed average from the
     * review children directly, so these properties are for legacy/compat consumers.
     */
    private static long[] recomputeAggregateRating(JCRNodeWrapper module, JCRNodeWrapper reviews)
            throws RepositoryException {
        long sum = 0L;
        long nb = 0L;
        final NodeIterator it = reviews.getNodes();
        while (it.hasNext()) {
            final Node n = it.nextNode();
            if (n.isNodeType(JNT_REVIEW) && n.hasProperty(PROP_RATING)) {
                sum += n.getProperty(PROP_RATING).getLong();
                nb++;
            }
        }
        if (!module.isNodeType(JMIX_RATING)) {
            module.addMixin(JMIX_RATING);
        }
        module.setProperty(PROP_SUM_OF_VOTES, sum);
        module.setProperty(PROP_NB_OF_VOTES, nb);
        return new long[]{sum, nb};
    }

    private static ActionResult error(int status, String message) throws Exception {
        final JSONObject json = new JSONObject();
        json.put("error", message);
        return new ActionResult(status, null, json);
    }

    /** Carries an HTTP status so the elevated callback can signal a precise client error. */
    private static final class ReviewException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final int status;

        ReviewException(int status, String message) {
            super(message);
            this.status = status;
        }

        int getStatus() {
            return status;
        }
    }
}
