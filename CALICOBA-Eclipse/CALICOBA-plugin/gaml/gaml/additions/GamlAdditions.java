package gaml.additions;
import msi.gaml.extensions.multi_criteria.*;
import msi.gama.outputs.layers.charts.*;
import msi.gama.outputs.layers.*;
import msi.gama.outputs.*;
import msi.gama.kernel.batch.*;
import msi.gama.kernel.root.*;
import msi.gaml.architecture.weighted_tasks.*;
import msi.gaml.architecture.user.*;
import msi.gaml.architecture.reflex.*;
import msi.gaml.architecture.finite_state_machine.*;
import msi.gaml.species.*;
import msi.gama.metamodel.shape.*;
import msi.gaml.expressions.*;
import msi.gama.metamodel.topology.*;
import msi.gaml.statements.test.*;
import msi.gama.metamodel.population.*;
import msi.gama.kernel.simulation.*;
import msi.gama.kernel.model.*;
import java.util.*;
import msi.gaml.statements.draw.*;
import  msi.gama.metamodel.shape.*;
import msi.gama.common.interfaces.*;
import msi.gama.runtime.*;
import java.lang.*;
import msi.gama.metamodel.agent.*;
import msi.gaml.types.*;
import msi.gaml.compilation.*;
import msi.gaml.factories.*;
import msi.gaml.descriptions.*;
import msi.gama.util.tree.*;
import msi.gama.util.file.*;
import msi.gama.util.matrix.*;
import msi.gama.util.graph.*;
import msi.gama.util.path.*;
import msi.gama.util.*;
import msi.gama.runtime.exceptions.*;
import msi.gaml.factories.*;
import msi.gaml.statements.*;
import msi.gaml.skills.*;
import msi.gaml.variables.*;
import msi.gama.kernel.experiment.*;
import msi.gaml.operators.*;
import msi.gama.common.interfaces.*;
import msi.gama.extensions.messaging.*;
import msi.gama.metamodel.population.*;
import msi.gaml.operators.Random;
import msi.gaml.operators.Maths;
import msi.gaml.operators.Points;
import msi.gaml.operators.Spatial.Properties;
import msi.gaml.operators.System;
import static msi.gaml.operators.Cast.*;
import static msi.gaml.operators.Spatial.*;
import static msi.gama.common.interfaces.IKeyword.*;
	@SuppressWarnings({ "rawtypes", "unchecked", "unused" })

public class GamlAdditions extends AbstractGamlAdditions {
	public void initialize() throws SecurityException, NoSuchMethodException {
	initializeAction();
	initializeSkill();
}public void initializeAction() throws SecurityException, NoSuchMethodException {
_action((s,a,t,v)->((fr.irit.smac.calicoba.gaml.skills.GlobalSkill) t).getInfluence(s),desc(PRIM,new Children(desc(ARG,NAME,"parameter_name",TYPE,"4","optional",FALSE),desc(ARG,NAME,"objective_name",TYPE,"4","optional",FALSE)),NAME,"get_influence",TYPE,Ti(D),VIRTUAL,FALSE),fr.irit.smac.calicoba.gaml.skills.GlobalSkill.class.getMethod("getInfluence",SC));
_action((s,a,t,v)->((fr.irit.smac.calicoba.gaml.skills.GlobalSkill) t).getObjective(s),desc(PRIM,new Children(desc(ARG,NAME,"objective_name",TYPE,"4","optional",FALSE)),NAME,"get_objective",TYPE,Ti(D),VIRTUAL,FALSE),fr.irit.smac.calicoba.gaml.skills.GlobalSkill.class.getMethod("getObjective",SC));
_action((s,a,t,v)->{((fr.irit.smac.calicoba.gaml.skills.GlobalSkill) t).setup(s);return null;},desc(PRIM,new Children(),NAME,"calicoba_setup",TYPE,Ti(void.class),VIRTUAL,FALSE),fr.irit.smac.calicoba.gaml.skills.GlobalSkill.class.getMethod("setup",SC));
_action((s,a,t,v)->((fr.irit.smac.calicoba.gaml.skills.GlobalSkill) t).getParameterAction(s),desc(PRIM,new Children(desc(ARG,NAME,"parameter_name",TYPE,"4","optional",FALSE)),NAME,"get_parameter_action",TYPE,Ti(I),VIRTUAL,FALSE),fr.irit.smac.calicoba.gaml.skills.GlobalSkill.class.getMethod("getParameterAction",SC));
_action((s,a,t,v)->{((fr.irit.smac.calicoba.gaml.skills.GlobalSkill) t).step(s);return null;},desc(PRIM,new Children(),NAME,"calicoba_step",TYPE,Ti(void.class),VIRTUAL,FALSE),fr.irit.smac.calicoba.gaml.skills.GlobalSkill.class.getMethod("step",SC));
_action((s,a,t,v)->{((fr.irit.smac.calicoba.gaml.skills.GlobalSkill) t).init(s);return null;},desc(PRIM,new Children(desc(ARG,NAME,"learn",TYPE,"3","optional",FALSE),desc(ARG,NAME,"alpha",TYPE,"2","optional",FALSE)),NAME,"calicoba_init",TYPE,Ti(void.class),VIRTUAL,FALSE),fr.irit.smac.calicoba.gaml.skills.GlobalSkill.class.getMethod("init",SC));
_action((s,a,t,v)->((fr.irit.smac.calicoba.gaml.skills.GlobalSkill) t).getObjectives(s),desc(PRIM,new Children(),NAME,"get_objectives",TYPE,Ti(GM),VIRTUAL,FALSE),fr.irit.smac.calicoba.gaml.skills.GlobalSkill.class.getMethod("getObjectives",SC));
}public void initializeSkill()  {
_skill("calicoba",fr.irit.smac.calicoba.gaml.skills.GlobalSkill.class,AS);
}
}