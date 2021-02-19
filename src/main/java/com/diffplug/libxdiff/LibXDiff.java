/*
 *  LibXDiff by Davide Libenzi ( File Differential Library )
 *  Copyright (C) 2003	Davide Libenzi
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, see
 *  <http://www.gnu.org/licenses/>.
 *
 *  Davide Libenzi <davidel@xmailserver.org>
 *  
 *  Ported to Java by Ned Twigg <ned.twigg@diffplug.com> on August 30 2019
 */
package com.diffplug.libxdiff;

import java.util.List;

public class LibXDiff {
	private LibXDiff () {}

	public static split_score score(List<CharSequence> lines, int start, int end) {
		split_score score = new split_score();
		score_add_split(measure_split(lines, end), score);
		score_add_split(measure_split(lines, start), score);
		return score;
	}

	/*
	 * Fill m with information about a hypothetical split of xdf above line split.
	 */
	private static split_measurement measure_split(List<CharSequence> lines, int split) {
		split_measurement m = new split_measurement();

		int i;

		if (split >= lines.size()) {
			m.end_of_file = 1;
			m.indent = -1;
		} else {
			m.end_of_file = 0;
			m.indent = get_indent(lines.get(split));
		}

		m.pre_blank = 0;
		m.pre_indent = -1;
		for (i = split - 1; i >= 0; i--) {
			m.pre_indent = get_indent(lines.get(i));
			if (m.pre_indent != -1) {
				break;
			}
			m.pre_blank += 1;
			if (m.pre_blank == MAX_BLANKS) {
				m.pre_indent = 0;
				break;
			}
		}

		m.post_blank = 0;
		m.post_indent = -1;
		for (i = split + 1; i < lines.size(); ++i) {
			m.post_indent = get_indent(lines.get(i));
			if (m.post_indent != -1) {
				break;
			}
			m.post_blank += 1;
			if (m.post_blank == MAX_BLANKS) {
				m.post_indent = 0;
				break;
			}
		}

		return m;
	}

	/**
	 * If more than this number of consecutive blank rows are found, just return this
	 * value. This avoids requiring O(N^2) work for pathological cases, and also
	 * ensures that the output of score_split fits in an int.
	 */
	private static final int MAX_BLANKS = 20;

	/**
	 * Return the amount of indentation of the specified line, treating TAB as 8
	 * columns. Return -1 if line is empty or contains only whitespace. Clamp the
	 * output value at MAX_INDENT.
	 */
	private static int get_indent(CharSequence line) {
		int i;
		int ret = 0;

		for (i = 0; i < line.length(); ++i) {
			char c = line.charAt(i);
			if (c == ' ') {
				ret += 1;
			} else if (c == '\t') {
				ret += 8 - ret % 8;
			} else {
				return ret;
			}
			/* ignore other whitespace characters */
			if (ret >= MAX_INDENT) {
				return MAX_INDENT;
			}
		}

		/* The line contains only whitespace. */
		return -1;
	}

	/**
	 * If a line is indented more than this, get_indent() just returns this value.
	 * This avoids having to do absurd amounts of work for data that are not
	 * human-readable text, and also ensures that the output of get_indent fits within
	 * an int.
	 */
	private static final int MAX_INDENT = 200;

	/** Characteristics measured about a hypothetical split position. */
	private static class split_measurement {
		/**
		 * Is the split at the end of the file (aside from any blank lines)?
		 */
		int end_of_file;

		/**
		 * How much is the line immediately following the split indented (or -1 if
		 * the line is blank):
		 */
		int indent;

		/**
		 * How many consecutive lines above the split are blank?
		 */
		int pre_blank;

		/**
		 * How much is the nearest non-blank line above the split indented (or -1
		 * if there is no such line)?
		 */
		int pre_indent;

		/**
		 * How many lines after the line following the split are blank?
		 */
		int post_blank;

		/**
		 * How much is the nearest non-blank line after the line following the
		 * split indented (or -1 if there is no such line)?
		 */
		int post_indent;
	}

	public static class split_score {
		/** The effective indent of this split (smaller is preferred). */
		int effective_indent;

		/** Penalty for this split (smaller is preferred). */
		int penalty;
	}

	/*
	 * The empirically-determined weight factors used by score_split() below.
	 * Larger values means that the position is a less favorable place to split.
	 *
	 * Note that scores are only ever compared against each other, so multiplying
	 * all of these weight/penalty values by the same factor wouldn't change the
	 * heuristic's behavior. Still, we need to set that arbitrary scale *somehow*.
	 * In practice, these numbers are chosen to be large enough that they can be
	 * adjusted relative to each other with sufficient precision despite using
	 * integer math.
	 */

	/** Penalty if there are no non-blank lines before the split */
	private static final int START_OF_FILE_PENALTY = 1;

	/** Penalty if there are no non-blank lines after the split */
	private static final int END_OF_FILE_PENALTY = 21;

	/** Multiplier for the number of blank lines around the split */
	private static final int TOTAL_BLANK_WEIGHT = -30;

	/** Multiplier for the number of blank lines after the split */
	private static final int POST_BLANK_WEIGHT = 6;

	/**
	 * Penalties applied if the line is indented more than its predecessor
	 */
	private static final int RELATIVE_INDENT_PENALTY = -4;
	private static final int RELATIVE_INDENT_WITH_BLANK_PENALTY = 10;

	/**
	 * Penalties applied if the line is indented less than both its predecessor and
	 * its successor
	 */
	private static final int RELATIVE_OUTDENT_PENALTY = 24;
	private static final int RELATIVE_OUTDENT_WITH_BLANK_PENALTY = 17;

	/**
	 * Penalties applied if the line is indented less than its predecessor but not
	 * less than its successor
	 */
	private static final int RELATIVE_DEDENT_PENALTY = 23;
	private static final int RELATIVE_DEDENT_WITH_BLANK_PENALTY = 17;

	/**
	 * We only consider whether the sum of the effective indents for splits are
	 * less than (-1), equal to (0), or greater than (+1) each other. The resulting
	 * value is multiplied by the following weight and combined with the penalty to
	 * determine the better of two scores.
	 */
	private static final int INDENT_WEIGHT = 60;

	/**
	 * How far do we slide a hunk at most?
	 */
	public static final int INDENT_HEURISTIC_MAX_SLIDING = 100;

	/**
	 * Compute a badness score for the hypothetical split whose measurements are
	 * stored in m. The weight factors were determined empirically using the tools and
	 * corpus described in
	 *
	 *     https://github.com/mhagger/diff-slider-tools
	 *
	 * Also see that project if you want to improve the weights based on, for example,
	 * a larger or more diverse corpus.
	 */
	private static void score_add_split(split_measurement m, split_score s) {
		/*
		 * A place to accumulate penalty factors (positive makes this index more
		 * favored):
		 */
		int post_blank, total_blank, indent;
		boolean any_blanks;

		if (m.pre_indent == -1 && m.pre_blank == 0) {
			s.penalty += START_OF_FILE_PENALTY;
		}

		if (m.end_of_file == 1) {
			s.penalty += END_OF_FILE_PENALTY;
		}

		/*
		 * Set post_blank to the number of blank lines following the split,
		 * including the line immediately after the split:
		 */
		post_blank = (m.indent == -1) ? 1 + m.post_blank : 0;
		total_blank = m.pre_blank + post_blank;

		/* Penalties based on nearby blank lines: */
		s.penalty += TOTAL_BLANK_WEIGHT * total_blank;
		s.penalty += POST_BLANK_WEIGHT * post_blank;

		if (m.indent != -1) {
			indent = m.indent;
		} else {
			indent = m.post_indent;
		}

		any_blanks = (total_blank != 0);

		/* Note that the effective indent is -1 at the end of the file: */
		s.effective_indent += indent;

		if (indent == -1) {
			/* No additional adjustments needed. */
		} else if (m.pre_indent == -1) {
			/* No additional adjustments needed. */
		} else if (indent > m.pre_indent) {
			/*
			 * The line is indented more than its predecessor.
			 */
			s.penalty += any_blanks ? RELATIVE_INDENT_WITH_BLANK_PENALTY : RELATIVE_INDENT_PENALTY;
		} else if (indent == m.pre_indent) {
			/*
			 * The line has the same indentation level as its predecessor.
			 * No additional adjustments needed.
			 */
		} else {
			/*
			 * The line is indented less than its predecessor. It could be
			 * the block terminator of the previous block, but it could
			 * also be the start of a new block (e.g., an "else" block, or
			 * maybe the previous block didn't have a block terminator).
			 * Try to distinguish those cases based on what comes next:
			 */
			if (m.post_indent != -1 && m.post_indent > indent) {
				/*
				 * The following line is indented more. So it is likely
				 * that this line is the start of a block.
				 */
				s.penalty += any_blanks ? RELATIVE_OUTDENT_WITH_BLANK_PENALTY : RELATIVE_OUTDENT_PENALTY;
			} else {
				/*
				 * That was probably the end of a block.
				 */
				s.penalty += any_blanks ? RELATIVE_DEDENT_WITH_BLANK_PENALTY : RELATIVE_DEDENT_PENALTY;
			}
		}
	}

	public static int score_cmp(split_score s1, split_score s2) {
		/* -1 if s1.effective_indent < s2->effective_indent, etc. */
		int cmp_indents = ((s1.effective_indent > s2.effective_indent ? 1 : 0) -
				(s1.effective_indent < s2.effective_indent ? 1 : 0));

		return INDENT_WEIGHT * cmp_indents + (s1.penalty - s2.penalty);
	}
}
