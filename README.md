# libxdiff: java port

This is a line-for-line port of the subset of LibXDiff which relates to sliding within-line diffs to account for brackets, quotes, etc.

The heuristics used to develop the weights are available at [`mhagger/diff-slider-tools`](https://github.com/mhagger/diff-slider-tools).

The exact lines translated are [lines 438-677](https://github.com/git/git/blob/791ad494835ac544d9e91573f3a65aeb97d126ed/xdiff/xdiffi.c#L438-L677) of the `xdiffi.c` file as it existed in `git/git` on August 17 2018. As of Oct 20 2020, none of the changes in upstream have affected the lines we care about.

# Changelog

## [Unreleased]

Originally translated to java on August 30 2019, but it sat dormant on a local branch for too long.

<!-- END CHANGELOG -->

# Acknowledgements

- Thanks [Davide Libenzi](https://github.com/davidel) for writing the part of LibXDiff which we use.
- Thanks to [Michael Haggerty](https://github.com/mhagger) for developing the heuristic weightings.
