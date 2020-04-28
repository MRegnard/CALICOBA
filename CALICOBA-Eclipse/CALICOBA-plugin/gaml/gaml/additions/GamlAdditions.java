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
	initializeSkill();
}public void initializeSkill()  {
_skill("reference_system",fr.irit.smac.calicoba.ReferenceSystemSkill.class,AS);
_skill("target_model",fr.irit.smac.calicoba.TargetModel.class,AS);
}
}