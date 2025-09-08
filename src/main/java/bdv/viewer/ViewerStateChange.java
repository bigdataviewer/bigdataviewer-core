/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.viewer;

/**
 * Types of BigDataViewer state changes that {@link ViewerStateChangeListener}s
 * can be notified about.
 *
 * @author Tobias Pietzsch
 */
public enum ViewerStateChange
{
	CURRENT_SOURCE_CHANGED,
	CURRENT_GROUP_CHANGED,
	SOURCE_ACTIVITY_CHANGED,
	GROUP_ACTIVITY_CHANGED,
	SOURCE_TO_GROUP_ASSIGNMENT_CHANGED,
	GROUP_NAME_CHANGED,
	NUM_SOURCES_CHANGED,
	NUM_GROUPS_CHANGED,
	VISIBILITY_CHANGED,
	DISPLAY_MODE_CHANGED,
	INTERPOLATION_CHANGED,
	ACCUMULATE_PROJECTOR_CHANGED,
	NUM_TIMEPOINTS_CHANGED,
	CURRENT_TIMEPOINT_CHANGED,
	VIEWER_TRANSFORM_CHANGED;
}
