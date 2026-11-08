package com.josheey3.panelpopout;

import com.josheey3.core.ui.WindowBounds;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persisted state of one popped-out panel, keyed by the sidebar tab's name.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class PopoutState
{
	private String name;
	private WindowBounds bounds;
	private boolean alwaysOnTop;
}
