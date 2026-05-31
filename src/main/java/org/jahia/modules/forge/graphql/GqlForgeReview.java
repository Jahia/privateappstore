package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

/**
 * The review created by {@code submitForgeReview} plus the resulting aggregate
 * rating for the module, so the client can update the UI without a round-trip
 * to re-read the node.
 */
@GraphQLName("ForgeReview")
@GraphQLDescription("A submitted forge module review and the module's resulting aggregate rating")
public class GqlForgeReview {

    private final String moduleId;
    private final int rating;
    private final String title;
    private final String comment;
    private final String author;
    private final long reviewCount;
    private final double averageRating;

    public GqlForgeReview(String moduleId, int rating, String title, String comment, String author,
                          long reviewCount, double averageRating) {
        this.moduleId = moduleId;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.author = author;
        this.reviewCount = reviewCount;
        this.averageRating = averageRating;
    }

    @GraphQLField
    @GraphQLDescription("UUID of the reviewed module/package")
    public String getModuleId() {
        return moduleId;
    }

    @GraphQLField
    @GraphQLDescription("The submitted rating, 1..5")
    public int getRating() {
        return rating;
    }

    @GraphQLField
    @GraphQLDescription("The submitted review title (may be null)")
    public String getTitle() {
        return title;
    }

    @GraphQLField
    @GraphQLDescription("The submitted review body (may be null)")
    public String getComment() {
        return comment;
    }

    @GraphQLField
    @GraphQLDescription("Username the review is attributed to")
    public String getAuthor() {
        return author;
    }

    @GraphQLField
    @GraphQLDescription("Total number of reviews on the module after this submission")
    public long getReviewCount() {
        return reviewCount;
    }

    @GraphQLField
    @GraphQLDescription("Average rating of the module after this submission")
    public double getAverageRating() {
        return averageRating;
    }
}
