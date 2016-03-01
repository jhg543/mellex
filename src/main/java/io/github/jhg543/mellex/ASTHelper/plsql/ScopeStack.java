package io.github.jhg543.mellex.ASTHelper.plsql;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ScopeStack {
	public ControlBlock ROOT = new ControlBlock();

	private Deque<ControlBlock> stack;

	private Map<String, ObjectDefinition> cache;

	private Deque<ControlBlock> blocks;

	public ScopeStack() {
		blocks = new LinkedList<ControlBlock>();
		stack = new LinkedList<ControlBlock>();
		cache = new HashMap<String, ObjectDefinition>();
		blocks.push(ROOT);
	}

	/**
	 * will set VariableDefinition.ControlBlock to new ControlBlock
	 * 
	 * @param decls
	 * @return
	 */
	public ControlBlock newBlock(List<ObjectDefinition> decls, boolean isParentVisible) {

		ControlBlock block = new ControlBlock();
		if (isParentVisible) {
			block.setParentBlock(blocks.peek());
		} else {
			cache.clear();
		}
		block.setDecls(decls);
		decls.forEach(vdef -> {
			vdef.setControlBlock(block);
		});
		blocks.push(block);
		return block;
	}

	public ControlBlock current() {
		return blocks.peek();
	}

	public ControlBlock pop() {
		ControlBlock c = blocks.pop();
		cache.clear();
		return c;
	}

	public <T extends ObjectDefinition> T searchByName(String s, Class<T> expectedClass) {
		ObjectDefinition v = cache.get(s);
		if (v != null) {
			return (T) v;
		}
		ControlBlock block = this.current();
		while (block != null) {
			Optional<ObjectDefinition> vdef = block.getDecls().stream().filter(vd -> vd.getName().equals(s)).findAny();
			if (vdef.isPresent()) {
				cache.put(s, vdef.get());
				return (T) vdef.get();
			}
			block = block.getParentBlock();
		}

		return null;
	}

}
