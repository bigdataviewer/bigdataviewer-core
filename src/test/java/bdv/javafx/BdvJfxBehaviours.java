package bdv.javafx;

import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.util.AbstractNamedBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

public class BdvJfxBehaviours
{

	public static final void install( Behaviours behaviours )
	{
		behaviours.namedBehaviour( new MyDragBehaviour( "drag1" ) );

		behaviours.namedBehaviour(
				new MyDragBehaviour( "drag2" ), "button1", "shift A | G" );

		behaviours.namedBehaviour(
				new MyScrollBehaviour( "scroll1" ), "alt scroll" );

		behaviours.namedBehaviour(
				new MyClickBehaviour( "click1" ), "button3", "B | all" );

		behaviours.namedBehaviour(
				new MyClickBehaviour( "click2" ), "button3 A" );

		behaviours.namedBehaviour(
				new MyClickBehaviour( "click3" ), "meta A double-click" );
	}

	private static class MyDragBehaviour extends AbstractNamedBehaviour implements DragBehaviour
	{
		public MyDragBehaviour( final String name )
		{
			super( name );
		}

		@Override
		public void init( final int x, final int y )
		{
			System.out.println( name() + ": init(" + x + ", " + y + ")" );
		}

		@Override
		public void drag( final int x, final int y )
		{
			System.out.println( name() + ": drag(" + x + ", " + y + ")" );
		}

		@Override
		public void end( final int x, final int y )
		{
			System.out.println( name() + ": end(" + x + ", " + y + ")" );
		}
	}

	private static class MyClickBehaviour extends AbstractNamedBehaviour implements ClickBehaviour
	{
		public MyClickBehaviour( final String name )
		{
			super( name );
		}

		@Override
		public void click( final int x, final int y )
		{
			System.out.println( name() + ": click(" + x + ", " + y + ")" );
		}
	}

	private static class MyScrollBehaviour extends AbstractNamedBehaviour implements ScrollBehaviour
	{
		public MyScrollBehaviour( final String name )
		{
			super( name );
		}

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			System.out.println( name() + ": scroll(" + wheelRotation + ", " + isHorizontal + ", " + x + ", " + y + ")" );
		}
	}

}
