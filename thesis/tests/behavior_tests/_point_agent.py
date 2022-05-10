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


class PointAgentTest(unittest.TestCase):
    def setUp(self):
        self.input = DummyDataInput()
        self.world = cobopti.CoBOpti(cobopti.CoBOptiConfig())
        self.world.add_parameter(self.input)
        self.param = self.world.get_agents_for_type(cobopti.agents.ParameterAgent)[0]

    def test_no_previous(self):
        agent = cobopti.agents.PointAgent('p', self.param, None, {'o': 0.5})
        self.world.add_agent(agent)
        agent.decide()

        messages = self.param.get_messages_for_type(cobopti.agents.VariationSuggestionMessage)
        self.assertEqual(1, len(messages))
        self.assertIs(agent, messages[0].sender)
        self.assertEqual(1, messages[0].direction)
        self.assertEqual(1, messages[0].steps_number)

    def test_1_previous__proceed(self):
        agent1 = cobopti.agents.PointAgent('p_1', self.param, None, {'o': 0.5})
        self.input.set_data(1)
        self.param.perceive()
        agent2 = cobopti.agents.PointAgent('p_2', self.param, agent1, {'o': 0.4})
        self.world.add_agent(agent1)
        self.world.add_agent(agent2)
        agent2.decide()

        messages = self.param.get_messages_for_type(cobopti.agents.VariationSuggestionMessage)
        self.assertEqual(1, len(messages))
        self.assertIs(agent2, messages[0].sender)
        self.assertEqual(1, messages[0].direction)
        self.assertAlmostEqual(4, messages[0].steps_number, places=6)

    def test_1_previous__go_back(self):
        agent1 = cobopti.agents.PointAgent('p_1', self.param, None, {'o': 0.5})
        self.input.set_data(1)
        self.param.perceive()
        agent2 = cobopti.agents.PointAgent('p_2', self.param, agent1, {'o': 0.6})
        self.world.add_agent(agent1)
        self.world.add_agent(agent2)
        agent2.decide()

        messages = self.param.get_messages_for_type(cobopti.agents.VariationSuggestionMessage)
        self.assertEqual(1, len(messages))
        self.assertIs(agent2, messages[0].sender)
        self.assertEqual(-1, messages[0].direction)
        self.assertAlmostEqual(6, messages[0].steps_number, places=6)

    def test_2_previous__follow_slope(self):
        agent1 = cobopti.agents.PointAgent('p_1', self.param, None, {'o': 0.5})
        self.input.set_data(0.5)
        self.param.perceive()
        agent2 = cobopti.agents.PointAgent('p_2', self.param, agent1, {'o': 0.4})
        self.input.set_data(1)
        self.param.perceive()
        agent3 = cobopti.agents.PointAgent('p_3', self.param, agent2, {'o': 0.3})
        self.world.add_agent(agent1)
        self.world.add_agent(agent2)
        self.world.add_agent(agent3)
        agent3.decide()

        messages = self.param.get_messages_for_type(cobopti.agents.VariationSuggestionMessage)
        self.assertEqual(1, len(messages))
        self.assertIs(agent3, messages[0].sender)
        self.assertEqual(1, messages[0].direction)
        self.assertAlmostEqual(3, messages[0].steps_number, places=6)

    def test_2_previous__go_to_middle(self):
        agent1 = cobopti.agents.PointAgent('p_1', self.param, None, {'o': 0.5})
        self.input.set_data(0.5)
        self.param.perceive()
        agent2 = cobopti.agents.PointAgent('p_2', self.param, agent1, {'o': 0.4})
        self.input.set_data(1)
        self.param.perceive()
        agent3 = cobopti.agents.PointAgent('p_3', self.param, agent2, {'o': 0.6})
        self.world.add_agent(agent1)
        self.world.add_agent(agent2)
        self.world.add_agent(agent3)
        agent3.decide()

        messages = self.param.get_messages_for_type(cobopti.agents.NewValueSuggestionMessage)
        self.assertEqual(1, len(messages))
        self.assertIs(agent3, messages[0].sender)
        self.assertEqual(0.75, messages[0].new_parameter_value)
        self.assertEqual(-1, messages[0].expected_criticality)
        self.assertFalse(messages[0].climbing)
