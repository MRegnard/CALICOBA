import unittest

import cobopti


class DummyDataOutput(cobopti.data_sources.DataOutput):
    def __init__(self):
        super().__init__(0, 2, 'out')

    def get_data(self):
        return 1


class DummyDataInput(cobopti.data_sources.DataInput):
    def __init__(self):
        super().__init__(0, 2, 'p')
        self._data = 0

    def get_data(self):
        return self._data

    def set_data(self, value):
        self._data = value


class DummyCriticalityFunction(cobopti.agents.ObjectiveFunction):
    def __init__(self):
        super().__init__('out')

    def __call__(self, **outputs_values):
        return 2 * outputs_values['out']


class AgentTestCase(unittest.TestCase):
    def setUp(self):
        self.agent = self.DummyAgent('agent')

    def test_get_and_set_world(self):
        c = cobopti.CoBOpti(cobopti.CoBOptiConfig())
        self.agent.world = c
        self.assertIs(c, self.agent.world)

    def test_get_and_set_id(self):
        self.agent.id = 1
        self.assertEqual(1, self.agent.id)

    def test_name(self):
        self.assertEqual('agent', self.agent.name)

    class DummyAgent(cobopti.agents.Agent):
        pass


class AgentWithDataSourceTestCase(unittest.TestCase):
    def setUp(self):
        self.out = DummyDataOutput()
        self.agent = self.DummyAgent(self.out)

    def test_perceive(self):
        self.agent.perceive()
        self.assertEqual(self.out.get_data(), self.agent.value)

    def test_inf(self):
        self.assertEqual(self.out.inf, self.agent.inf)

    def test_sup(self):
        self.assertEqual(self.out.sup, self.agent.sup)

    def test_on_message(self):
        m = self.DummyMessage(self.DummyAgent(DummyDataOutput()))
        self.agent.on_message(m)
        messages = self.agent.get_messages_for_type(self.DummyMessage)
        self.assertEqual(1, len(messages))
        self.assertIn(m, messages)

    class DummyAgent(cobopti.agents.AgentWithDataSource[DummyDataOutput]):
        pass

    class DummyMessage(cobopti.agents.Message):
        pass


class ObjectiveAgentTestCase(unittest.TestCase):
    def setUp(self):
        c = cobopti.CoBOpti(cobopti.CoBOptiConfig())
        self.out = DummyDataOutput()
        c.add_output(self.out)
        self.output_agent = c.get_agents_for_type(cobopti.agents.OutputAgent)[0]
        self.function = DummyCriticalityFunction()
        c.add_objective('obj_out', self.function)
        self.obj_agent = c.get_agents_for_type(cobopti.agents.ObjectiveAgent)[0]

    def test_name(self):
        self.assertEqual('obj_out', self.obj_agent.name)

    def test_get_init_criticality(self):
        self.assertEqual(0, self.obj_agent.criticality)

    def test_criticality(self):
        self.output_agent.perceive()
        self.obj_agent.perceive()
        self.obj_agent.decide()
        self.assertEqual(1, self.obj_agent.criticality)


class OutputAgentTestCase(unittest.TestCase):
    def setUp(self):
        self.out = DummyDataOutput()
        self.agent = cobopti.agents.OutputAgent(self.out)

    def test_name(self):
        self.assertEqual(self.out.name, self.agent.name)

    def test_value(self):
        self.agent.perceive()
        self.assertEqual(self.out.get_data(), self.agent.value)

    def test_inf(self):
        self.assertEqual(self.out.inf, self.agent.inf)

    def test_sup(self):
        self.assertEqual(self.out.sup, self.agent.sup)


class ParameterAgentTestCase(unittest.TestCase):
    def setUp(self):
        self.input = DummyDataInput()
        self.agent = cobopti.agents.ParameterAgent(self.input)
        self.agent.world = cobopti.CoBOpti(cobopti.CoBOptiConfig())

    def test_name(self):
        self.assertEqual(self.input.name, self.agent.name)

    def test_value(self):
        self.agent.perceive()
        self.assertEqual(self.input.get_data(), self.agent.value)

    def test_inf(self):
        self.assertEqual(self.input.inf, self.agent.inf)

    def test_sup(self):
        self.assertEqual(self.input.sup, self.agent.sup)
