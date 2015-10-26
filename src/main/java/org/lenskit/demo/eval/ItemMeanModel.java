/* This file may be freely modified, used, and redistributed without restriction. */
package org.lenskit.demo.eval;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import org.grouplens.grapht.annotation.DefaultProvider;
import org.lenskit.baseline.MeanDamping;
import org.lenskit.inject.Shareable;
import org.lenskit.util.keys.Long2DoubleSortedArrayMap;

import java.io.Serializable;

/**
 * Model that maintains the mean offset from the global mean for the ratings
 * for each item.
 *
 * These offsets can be used for predictions by calling the {@link #getGlobalMean()}
 * and {@link #getItemOffsets()} methods.
 *
 * <p>These computations support mean smoothing (see {@link MeanDamping}).
 *
 * Users of this model will usually call the Provider's get method to create
 * a suitable model.  The model can be kept around until recomputation is necessary.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
@Shareable
@DefaultProvider(ItemMeanModelBuilder.class)
public class ItemMeanModel implements Serializable {
    private static final long serialVersionUID = 2L;

    private final double globalMean;
    private final Long2DoubleMap itemOffsets;

    public ItemMeanModel(double global, Long2DoubleMap items) {
        itemOffsets = new Long2DoubleSortedArrayMap(items);
        globalMean = global;
    }

    /**
     * Get the global mean rating.
     * @return The global mean rating.
     */
    public double getGlobalMean() {
        return globalMean;
    }

    /**
     * Get the vector of item mean offsets.
     *
     * @return The vector of item mean offsets.  These are mean offsets from the global mean rating,
     *         so add the global mean to each rating to get the item mean.
     */
    public Long2DoubleMap getItemOffsets() {
        return itemOffsets;
    }

    /**
     * Get the offset for an item. This is the difference between the item's mean rating and the
     * global mean.
     * @return The item offset.  This will be 0 for unknown items.
     */
    public double getItemOffset(long item) {
        return itemOffsets.get(item);
    }

}
