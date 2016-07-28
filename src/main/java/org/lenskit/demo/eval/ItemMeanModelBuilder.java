/* This file may be freely modified, used, and redistributed without restriction. */
package org.lenskit.demo.eval;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.lenskit.baseline.MeanDamping;
import org.lenskit.data.dao.EventDAO;
import org.lenskit.data.ratings.Rating;
import org.lenskit.inject.Transient;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
* @author <a href="http://www.grouplens.org">GroupLens Research</a>
*/
public class ItemMeanModelBuilder implements Provider<ItemMeanModel> {
    private static final Logger logger = LoggerFactory.getLogger(ItemMeanModelBuilder.class);
    private double damping = 0;
    private EventDAO dao;

    /**
     * Construct a new provider.
     *
     * @param dao The DAO.  This is {@link Transient}, meaning that it will be used to build the
     *            model but the model, once built, does not depend on it.
     * @param d   The Bayesian mean damping term for item means. A positive value biases means
     *            towards the global mean.
     */
    @Inject
    public ItemMeanModelBuilder(@Transient EventDAO dao, @MeanDamping double d) {
        this.dao = dao;
        damping = d;
    }

    /**
     * Construct the item mean model.
     * @return The item mean model.
     */
    @Override
    public ItemMeanModel get() {
        // We iterate the loop to compute the global and per-item mean
        // ratings.  Subtracting the global mean from each per-item mean
        // is equivalent to averaging the offsets from the global mean, so
        // we can compute the means in parallel and subtract after a single
        // pass through the data.
        double total = 0.0;
        int count = 0;
        // map to sum item ratings
        Long2DoubleMap itemRatingSums = new Long2DoubleOpenHashMap();
        // fastutil lets us specify a default value; this makes sums easier to accumulate
        itemRatingSums.defaultReturnValue(0.0);
        // map to count item ratings
        Long2IntMap itemRatingCounts = new Long2IntOpenHashMap();
        itemRatingCounts.defaultReturnValue(0);

        try (ObjectStream<Rating> ratings = dao.streamEvents(Rating.class)) {
            for (Rating rating: ratings) {
                if (!rating.hasValue()) {
                    continue; // skip unrates
                }

                long i = rating.getItemId();
                double v = rating.getValue();
                total += v;
                count++;
                itemRatingSums.put(i, v + itemRatingSums.get(i));
                itemRatingCounts.put(i, 1 + itemRatingCounts.get(i));
            }
        }

        final double mean = count > 0 ? total / count : 0;
        logger.debug("Computed global mean {} for {} items",
                     mean, itemRatingSums.size());

        logger.debug("Computing item offsets, damping={}", damping);
        // create a map to hold item mean offsets
        Long2DoubleMap offsets = new Long2DoubleOpenHashMap(itemRatingSums.size());
        // now iterate over our items
        for (Long2DoubleMap.Entry e: itemRatingSums.long2DoubleEntrySet()) {
            final long iid = e.getKey();
            // compute the damped item mean
            final double itemCount = itemRatingCounts.get(iid) + damping;
            final double itemTotal = e.getDoubleValue() + damping * mean;
            if (itemCount > 0) {
                offsets.put(iid, (itemTotal / itemCount) - mean);
            }
        }

        return new ItemMeanModel(mean, offsets);
    }
}
