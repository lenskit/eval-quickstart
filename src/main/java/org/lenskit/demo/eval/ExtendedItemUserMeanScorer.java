/* This file may be freely modified, used, and redistributed without restriction. */
package org.lenskit.demo.eval;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.baseline.MeanDamping;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.UserEventDAO;
import org.lenskit.data.history.History;
import org.lenskit.data.history.UserHistory;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.Ratings;
import org.lenskit.results.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Scorer that returns the user's mean offset from item mean rating for all
 * scores.
 *
 * <p>This implements the baseline scorer <i>p<sub>u,i</sub> = mu + b<sub>i</sub> +
 * b<sub>u</sub></i>, where <i>b<sub>i</sub></i> is the item's average rating (less the global
 * mean <i>mu</i>), and <i>b<sub>u</sub></i> is the user's average offset (the average
 * difference between their ratings and the item-mean baseline).
 *
 * <p>It supports mean smoothing (see {@link MeanDamping}).
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class ExtendedItemUserMeanScorer extends AbstractItemScorer {
    @SuppressWarnings("unused")
    private static final long serialVersionUID = 22L;
    private static final Logger logger = LoggerFactory.getLogger(ExtendedItemUserMeanScorer.class);

    private final UserEventDAO dao;
    private final double userDamping;  // damping for computing the user averages; more damping biases toward global.
    private final ItemMeanModel model;

    /**
     * Create a new scorer, this assumes ownership of the given map.
     *
     * @param dao The user-event DAO.
     * @param inModel The model.
     * @param inUserDamping The damping term.
     */
    @Inject
    public ExtendedItemUserMeanScorer(UserEventDAO dao, ItemMeanModel inModel,
                                      @MeanDamping double inUserDamping) {
        this.dao = dao;
        model = inModel;
        userDamping = inUserDamping;
    }

    /**
     * Compute the mean offset in user rating from item mean rating.
     *
     * @param ratings the user's rating profile
     * @return the mean offset from item mean rating.
     */
    protected double computeUserOffset(Long2DoubleMap ratings) {
        if (ratings.isEmpty()) {
            return 0;
        }

        // we want to compute the average of the user's offset from item mean
        double sum = 0;
        int n = 0;
        for (Long2DoubleMap.Entry e: ratings.long2DoubleEntrySet()) {
            long item = e.getKey();
            sum += e.getDoubleValue() - model.getGlobalMean() - model.getItemOffset(item);
            n += 1;
        }

        // now return the damped mean
        return sum / (n + userDamping);
    }

    @Nonnull
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        logger.debug("score called to attempt to score %d elements", items.size());

        // Get the user's profile
        UserHistory<Rating> profile = dao.getEventsForUser(user, Rating.class);
        if (profile == null) {
            profile = History.forUser(user, Collections.<Rating>emptyList());
        }

        // Convert the user's profile into a rating vector
        Long2DoubleMap ratings = Ratings.userRatingVector(profile);
        double meanOffset = computeUserOffset(ratings);
        double baseScore = model.getGlobalMean() + meanOffset;

        // Accumulate a list of results (scores).
        List<Result> results = new ArrayList<>();
        for (long item: items) {
            double score = baseScore + model.getItemOffsets().get(item);
            results.add(Results.create(item, score));
        }

        // Convert this list ot a result map and return
        return Results.newResultMap(results);
    }
}
