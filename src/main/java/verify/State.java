package verify;

public enum State
{
	A,B,C,D,E,F;

	public static int[][] defaultTransitions = new int[][] {
		{ 1, 1, 1, 0, 0, 0 },
		{ 0, 1, 1, 1, 0, 0 },
		{ 0, 0, 1, 1, 0, 0 },
		{ 0, 0, 0, 1, 1, 0 },
		{ 0, 0, 0, 0, 1, 1 },
		{ 0, 0, 0, 0, 0, 1 },
	};
}
