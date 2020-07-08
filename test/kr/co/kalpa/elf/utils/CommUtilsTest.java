package kr.co.kalpa.elf.utils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CommUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		String s = CommUtils.padHttp("abc");
		assertTrue(s.equals("http://abc"));
	}

}
