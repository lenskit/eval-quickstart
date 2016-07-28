/* This file may be freely modified, used, and redistributed without restriction. */
package org.lenskit.demo.eval;

import it.unimi.dsi.fastutil.longs.LongSortedSet;
import org.grouplens.lenskit.collections.LongUtils;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.junit.Before;
import org.junit.Test;
import org.lenskit.api.ItemScorer;
import org.lenskit.data.dao.EventCollectionDAO;
import org.lenskit.data.dao.EventDAO;
import org.lenskit.data.dao.PrefetchingUserEventDAO;
import org.lenskit.data.dao.UserEventDAO;
import org.lenskit.data.ratings.Rating;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * Test baseline Scorers that compute means from data.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class MeanScorerTest {
    private EventDAO dao;
    private UserEventDAO ueDAO;

    @Before
    public void createRatingSource() {
        List<Rating> rs = new ArrayList<>();
        rs.add(Rating.create(1, 5, 3));
        rs.add(Rating.create(1, 7, 4));
        rs.add(Rating.create(8, 4, 5));
        rs.add(Rating.create(8, 5, 4));

        // Global Mean: 16 / 4 = 4

        // Item  Means  Offsets
        // 5 ->   3.5     -0.5 
        // 7 ->   4.0      0.0
        // 4 ->   5.0      1.0

        // User  Offset Avg
        //  1      -0.5+0.0 / 2 = -0.25
        //  8      0.0+0.5 / 2  = 0.25

        // Preds
        // u1 on i5 -> 3.25
        // u1 on i7 -> 3.75
        // u1 on i10 -> unable to predict
        // u1 on i4 -> 4.75
        // u8 on i5 -> 3.75
        // u8 on i7 -> 4.25
        // u8 on i4 -> 5.25 (?)
        // u2 on i4 -> 5.0
        // u2 on i7 -> 4.0
        // u2 on i5 -> 3.5
        dao = EventCollectionDAO.create(rs);
        ueDAO = new PrefetchingUserEventDAO(dao);
    }

    LongSortedSet itemSet(long item) {
        return LongUtils.packedSet(item);
    }

    @Test
    public void testUserItemMeanScorer() {
        ItemMeanModel model = new ItemMeanModelBuilder(dao, 0).get();
        ItemScorer scorer = new ExtendedItemUserMeanScorer(ueDAO, model, 0);

        assertThat(model.getGlobalMean(), closeTo(4.0, 1.0e-6));
        assertThat(model.getItemOffset(5), closeTo(-0.5, 1.0e-6));
        assertThat(model.getItemOffset(7), closeTo(0, 1.0e-6));
        assertThat(model.getItemOffset(4), closeTo(1.0, 1.0e-6));

        long[] items = {5, 7, 10};
        double[] ratings = {3, 6, 4};

        // User 1
        MutableSparseVector scores1 = MutableSparseVector.wrap(items, ratings); // ratings ignored
        Map<Long,Double> results = scorer.score(1L, LongUtils.packedSet(items));
        assertThat(results.get(5L), closeTo(3.25, 1.0e-5));
        assertThat(results.get(7L), closeTo(3.75, 1.0e-5));
        assertThat(results.get(10L), closeTo(3.75, 1.0e-5));  // user overall average
        assertFalse(results.containsKey(4L));

        // User 8
        long[] items8 = {4, 5, 7};

        results = scorer.score(8L, LongUtils.packedSet(items8));
        assertThat(results.get(5L), closeTo(3.75, 1.0e-5));
        assertThat(results.get(7L), closeTo(4.25, 1.0e-5));
        assertThat(results.get(4L), closeTo(5.25, 1.0e-5));

        // User 2, not in the set of users in the DAO
        results = scorer.score(2L, LongUtils.packedSet(items));
        assertThat(results.get(5L), closeTo(3.5, 1.0e-5));
        assertFalse(results.containsKey(4L));
        assertThat(results.get(7L), closeTo(4, 1.0e-5));
    }
}
