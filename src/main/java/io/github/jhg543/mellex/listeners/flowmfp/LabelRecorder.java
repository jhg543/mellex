package io.github.jhg543.mellex.listeners.flowmfp;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LabelRecorder {
	private Map<String,Instruction> labels;
	private Map<String,List<Instruction>> breakLabels;
	private Map<String,List<Instruction>> continueLabels;
	private Deque<List<Instruction>> currentBreaks;
	private Deque<List<Instruction>> currentContinues;
	
	public LabelRecorder()
	{
		labels = new HashMap<>();
		breakLabels = new HashMap<>();
		continueLabels = new HashMap<>();
		currentBreaks = new LinkedList<>();
		currentContinues = new LinkedList<>();
	}

	public Map<String, Instruction> getLabels() {
		return labels;
	}

	public Map<String, List<Instruction>> getBreakLabels() {
		return breakLabels;
	}

	public Map<String, List<Instruction>> getContinueLabels() {
		return continueLabels;
	}

	public List<Instruction> getCurrentBreaks() {
		return currentBreaks.peek();
	}

	public List<Instruction> getCurrentContinues() {
		return currentContinues.peek();
	}
	
	public void enterLoop()
	{
		// TODO will subprocedure defined in loop?
		currentBreaks.push(new ArrayList<>());
		currentContinues.push(new ArrayList<>());
	}
	
	public void exitLoop()
	{
		currentBreaks.pop();
		currentContinues.pop();
	}
}
