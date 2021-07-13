package fr.irit.smac.calicoba.mas.agents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.Calicoba;

class AgentTest {
  private Agent agent;

  @BeforeEach
  void setUp() throws Exception {
    this.agent = new Agent() {
    };
  }

  @Test
  void testGetAndSetWorld() {
    Calicoba w = new Calicoba(false, null, false, 0);
    this.agent.setWorld(w);
    Assertions.assertTrue(w == this.agent.getWorld());
  }

  @Test
  void testGetAndSetId() {
    String id = "id";
    this.agent.setId(id);
    Assertions.assertEquals(id, this.agent.getId());
  }
}
