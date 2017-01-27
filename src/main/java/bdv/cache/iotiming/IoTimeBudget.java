package bdv.cache.iotiming;

/**
 * Budget of time that can be spent in blocking IO. The budget is grouped by
 * priority levels, where level 0 is the highest priority. The budget for
 * level <em>i &gt; j</em> must always be smaller-equal the budget for level
 * <em>j</em>.
 *
 * For BDV, the time unit of {@link IoTimeBudget} values is nanoseconds.
 */
public class IoTimeBudget
{
	private long[] budget;

	public IoTimeBudget()
	{
		budget = new long[] { 0 };
	}

	/**
	 * (Re-)initialize the IO time budget, that is, the time that can be spent
	 * in blocking IO.
	 *
	 * @param partialBudget
	 *            Initial budget for priority levels <em>0</em> through
	 *            <em>n</em>. The budget for level <em>i&gt;j</em> must always
	 *            be smaller-equal the budget for level <em>j</em>.
	 */
	public synchronized void reset( final long[] partialBudget )
	{
		if ( partialBudget == null || partialBudget.length == 0 )
			clear();
		else
		{
			if ( partialBudget.length == budget.length )
				System.arraycopy( partialBudget, 0, budget, 0, budget.length );
			else
				budget = partialBudget.clone();

			for ( int i = 1; i < budget.length; ++i )
				if ( budget[ i ] > budget[ i - 1 ] )
					budget[ i ] = budget[ i - 1 ];
		}
	}

	/**
	 * Set the budget to 0 (for all levels).
	 */
	public synchronized void clear()
	{
		for ( int i = 0; i < budget.length; ++i )
			budget[ i ] = 0;
	}

	/**
	 * Returns how much time is left for the specified priority level.
	 *
	 * @param level
	 *            priority level. must be greater &ge; 0.
	 * @return time left for the specified priority level.
	 */
	public synchronized long timeLeft( final int level )
	{
		final int blevel = Math.min( level, budget.length - 1 );
		return budget[ blevel ];
	}

	/**
	 * Use the specified amount of time of the specified level.
	 *
	 * {@code t} is subtracted from the budgets of level {@code level} and
	 * smaller. If by this, the remaining budget of {@code level} becomes
	 * smaller than the remaining budget of {@code level + 1}, then this is
	 * reduced too. (And the same for higher levels.)
	 *
	 * @param t
	 *            how much time to use.
	 * @param level
	 *            priority level. must be greater &ge; 0.
	 */
	public synchronized void use( final long t, final int level )
	{
		final int blevel = Math.min( level, budget.length - 1 );
		int l = 0;
		for ( ; l <= blevel; ++l )
			budget[ l ] -= t;
		for ( ; l < budget.length && budget[ l ] > budget[ l - 1 ]; ++l )
			budget[ l ] = budget[ l - 1 ];
	}
}
