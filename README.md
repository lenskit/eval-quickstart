# LensKit Evaluation Quickstart

This example project shows how to run and evaluate a custom LensKit recommender component using
Gradle, the current recommended way to run LensKit evaluations.  It is intended to serve as a
template for you to use when you create new LensKit evaluations.

The key user files that you are likely to want to edit are:

-   `build.gradle`: to configure the build, add dependencies, etc.
-   `eval.groovy`: to change the lenskit evaluation that is run,
    perhaps by configuring different recommenders.
=   `chart.py`: to change the analysis of the output data in `build`,
    perhaps including the charts that are generated.
  
To run the evaluation, run:

    ./gradlew evaluate

As is typical with Gradle projects, all output files go in the `build` directory, where they can
be removed with `./gradlew clean`.

## Example Scorer

There is a simple example scorer in src/main/java.  This scorer
includes a model that generates item mean ratings, and a scorer based on
that model.  You may find the model and predictor useful as starting
points for your own predictors.  The analysis script uses this
scorer along with some well-known rating prediction algorithms.

# More Information

More information on LensKit and its evaluator can be found on the [LensKit web site][web].

[web]: http://lenskit.org/documentation/

# Copyright

This project was created by the LensKit contributors.

The files in this project may be freely modified, used, and distributed without restriction.

If further legal clarity is required, these files are licensed under [Creative Commons CC0][CC0].

[CC0]: https://creativecommons.org/publicdomain/zero/1.0/
