import org.lenskit.baseline.BaselineScorer
import org.lenskit.baseline.ItemMeanRatingItemScorer
import org.lenskit.knn.item.ItemItemScorer
import org.lenskit.transform.normalize.BaselineSubtractingUserVectorNormalizer
import org.lenskit.transform.normalize.UserVectorNormalizer

bind ItemScorer to ItemItemScorer
bind UserVectorNormalizer to BaselineSubtractingUserVectorNormalizer
within (UserVectorNormalizer) {
    bind (BaselineScorer, ItemScorer) to ItemMeanRatingItemScorer
}
