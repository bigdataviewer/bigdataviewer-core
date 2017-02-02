package verify;

public enum State
{
	A,B,C,D,E;

	public static int[][] defaultTransitions = new int[][] {
		{ 1, 1, 0, 0, 0 },
		{ 0, 1, 1, 0, 0 },
		{ 0, 0, 1, 1, 0 },
		{ 0, 0, 0, 1, 1 },
		{ 0, 0, 0, 0, 1 },
	};
}
