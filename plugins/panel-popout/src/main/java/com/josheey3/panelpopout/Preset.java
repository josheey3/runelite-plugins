package com.josheey3.panelpopout;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A named set of panels to pop out together, with each window's position where it was saved.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class Preset
{
	private String name;
	private List<PopoutState> panels = new ArrayList<>();
}
