package org.verdictdb.core.querying;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecutableNodeBaseTest {

  @Test
  public void testEquals() {
    ExecutableNodeBase a = new ExecutableNodeBase();
    ExecutableNodeBase b = a;
    assertEquals(b, a);
  }
  
  @Test
  public void testContains() {
    Map<ExecutableNodeBase, Integer> mymap = new HashMap<>();
    ExecutableNodeBase a = new ExecutableNodeBase();
    mymap.put(a, 3);
    assertTrue(mymap.containsKey(a));
  }

}
