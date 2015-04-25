/* This file may be freely modified, used, and redistributed without restriction. */
import org.grouplens.lenskit.baseline.BaselineScorer
import org.grouplens.lenskit.baseline.ItemMeanRatingItemScorer
import org.grouplens.lenskit.baseline.UserMeanBaseline
import org.grouplens.lenskit.baseline.UserMeanItemScorer
import org.grouplens.lenskit.eval.data.crossfold.RandomOrder
import org.grouplens.lenskit.eval.metrics.predict.NDCGPredictMetric
import org.grouplens.lenskit.eval.metrics.predict.RMSEPredictMetric
import org.grouplens.lenskit.knn.item.ItemItemScorer
import org.grouplens.lenskit.transform.normalize.BaselineSubtractingUserVectorNormalizer
import org.grouplens.lenskit.transform.normalize.UserVectorNormalizer

trainTest("eval") {
    // options can be listed here in any order. This order happens to make sense to me.

    // this file will contain summary level results for each algorithm and each crossfold.
    output "build/eval-results.csv"
    // this file will contain metric results for every user. This is useful in careful statistical analysis.
    userOutput "build/eval-user.csv"

    // these line are optional, having them will enable disk based cacheing.
    // This means if you re-run an analysis without changing the configuration, lenskit won't have
    // to rebuild algorithm models, and can instead load the pre-built models from disk.
    componentCacheDirectory "build/componentCache"
    cacheAllComponents true

    // add metrics to the evaluation. To find the name of metrics in lenskit check out org.grouplens.lenskit.eval.metrics
    metric RMSEPredictMetric
    metric NDCGPredictMetric

    // add datasets to the evaluation.
    // strictly speaking, this adds all five crossfold splits as _seperate_ datasets.
    // this can be repeated if you want to evaluate on multiple datasets at the same time. Output
    // will contain a "dataset" row.
    dataset crossfold("ML100K") {
        source csvfile {
            file "build/ml-100k/u.data"
            delimiter "\t"
            domain {
                minimum 1
                maximum 5
                precision 1
            }
            order RandomOrder    // choose test ratings randomly
            holdoutFraction 0.2  // holdout one fifth of test users' ratings
            partitions 5         // make five partitions
            train 'build/crossfold/train.%d.pack'
            test 'build/crossfold/test.%d.pack'
            // For more options look at org.grouplens.lenskit.eval.data.crossfold.CrossfoldTask
            // commands here call setters on that object (so to tell it to write timestamps we want
            // to call setWriteTimestamps so we can put 'writeTimestamps true' in this file
        }
    }

    // configure algorithms.
    // Lenskit uses a dependency injection framework, This system lets you declaratively describe
    // the algorithm you want (rather than having to call constructors and factory methods manually)
    // I find that this idea takes a little getting used to, but I think its easier to build objects
    // this way most of the time.
    // more information on algorithm can be found on lenskit.org
    // http://lenskit.org/documentation/basics/structure/ - basic java types for recommending
    // http://lenskit.org/documentation/algorithms/       - links to specific algorithm information including common configurations

    // "personalized" mean. This baseline algorithm learns an average rating for each item, and then an offset for each user.
    algorithm("PersMean") {
        bind ItemScorer to UserMeanItemScorer
        bind (UserMeanBaseline, ItemScorer) to ItemMeanRatingItemScorer
    }

    // Standard item-item CF.
    algorithm("ItemItem") {
        bind ItemScorer to ItemItemScorer
        bind UserVectorNormalizer to BaselineSubtractingUserVectorNormalizer
        within (UserVectorNormalizer) {
            bind (BaselineScorer, ItemScorer) to ItemMeanRatingItemScorer
        }
    }
}
