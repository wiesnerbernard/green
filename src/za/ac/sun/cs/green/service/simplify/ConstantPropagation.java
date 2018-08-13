package za.ac.sun.cs.green.service.simplify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.service.BasicService;
import za.ac.sun.cs.green.util.Reporter;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;

public class ConstantPropagation extends BasicService {
  	private int invocations = 0;

	public ConstantPropagation(Green solver) {
		super(solver);
	}
  
  	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Set<Instance> result = (Set<Instance>) instance.getData(getClass());
		if (result == null) {
			final Map<Variable, Variable> map = new HashMap<Variable, Variable>();
			final Expression e = propagate(instance.getFullExpression(), map);
			final Instance i = new Instance(getSolver(), instance.getSource(), null, e);
			result = Collections.singleton(i);
			instance.setData(getClass(), result);
		}
		return result;
	}
  
  	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocations = " + invocations);
	}
  
  	public Expression propagate(Expression expression,
			Map<Variable, Variable> map) {
		try {
			log.log(Level.FINEST, "Before Propagation: " + expression);
			invocations++;
			OrderingVisitor orderingVisitor = new OrderingVisitor();
			expression.accept(orderingVisitor);
			expression = orderingVisitor.getExpression();
			//PropagationVisitor propagationVisitor = new PropagationVisitor();
			//expression.accept(propagationVisitor);
			//Expression propagated = propagationVisitor.getExpression();
			//if (propagated != null) {
				//propagated = new Renamer(map,
				//		propagationVisitor.getVariableSet()).rename(propagated);
			//}
			log.log(Level.FINEST, "After Propagation: " + expression);
			return expression;
			//return propagated;
		} catch (VisitorException x) {
			log.log(Level.SEVERE,
					"encountered an exception -- this should not be happening!",
					x);
		}
		return null;
	}

	private static class OrderingVisitor extends Visitor {
		
		private Map<IntVariable, IntConstant> map;
		private Stack<Expression> stack;

		public OrderingVisitor() {
			stack = new Stack<Expression>();
			map = new HashMap<IntVariable, IntConstant>();
		}

		public Expression getExpression() {
			return stack.pop();
		}

		@Override
		public void postVisit(IntConstant constant) {
			stack.push(constant);
		}

		@Override
		public void postVisit(IntVariable variable) {
			stack.push(variable);
		}

		@Override
		public void postVisit(Operation operation) throws VisitorException {
			Operation.Operator op = operation.getOperator();
			if (stack.size() >= 2) {
				Expression right = stack.pop();
				Expression left = stack.pop();
				if (op == Operation.Operator.EQ) {
					
					if (right instanceof IntConstant && left instanceof IntVariable) {
						map.put((IntVariable) left, (IntConstant) right);
					}
					
					Operation nop = new Operation(operation.getOperator(), left, right);
					System.out.println(">>>>>>>>>>>>>>>>>>>>>>"+left + " "+ nop + " " + right+"<<<<<<<<<<<<<<<<<<<<<");

					stack.push(nop);
				} else if (left instanceof IntVariable || right instanceof IntVariable) {
					if (map.containsKey(left)) {
						 left = map.get(left);
					} else if (map.containsKey(right)) {
						right = map.get(right);	
					}
					Operation nop = new Operation(operation.getOperator(), left, right);
					stack.push(nop);
				} else {
					Operation nop = new Operation(operation.getOperator(), left, right);
					stack.push(nop);
				}
			} else {
				 for (int i = op.getArity(); i > 0; i--) {
                    			stack.pop();
                		}
                		stack.push(operation);
			}
		}
	}
}
