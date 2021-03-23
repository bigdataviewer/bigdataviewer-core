/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.ui.appearance;

import bdv.tools.brightness.ColorIcon;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Helpers for building settings pages:
 * Checkboxes, color-selection icons, ...
 */
// TODO: Polish a bit and make public.
//       This is a modified version of the StyleElements class from Mastodon.
//       Currently it's only used in AppearanceSettingsPage.
//       Eventually this should be unified with the Mastodon one and reused.
class StyleElements
{
	public static Separator separator()
	{
		return new Separator();
	}

	public static BooleanElement booleanElement( final String label, final BooleanSupplier get, final Consumer< Boolean > set )
	{
		return new BooleanElement( label )
		{
			@Override
			public boolean get()
			{
				return get.getAsBoolean();
			}

			@Override
			public void set( final boolean b )
			{
				set.accept( b );
			}
		};
	}

	public static ColorElement colorElement( final String label, final IntSupplier get, final IntConsumer set )
	{
		return new ColorElement( label )
		{
			@Override
			public int getColor()
			{
				return get.getAsInt();
			}

			@Override
			public void setColor( final int c )
			{
				set.accept( c );
			}
		};
	}

	public static < T > ComboBoxElement< T > comboBoxElement( final String label,
			final Supplier< T > get, final Consumer< T > set,
			final ComboBoxEntry< T >... entries )
	{
		return comboBoxElement( label, get, set, Arrays.asList( entries ) );
	}

	public static < T extends Enum< T > > ComboBoxElement< T > comboBoxElement( final String label,
			final Supplier< T > get, final Consumer< T > set,
			final T[] entries )
	{
		final List< ComboBoxEntry< T > > list = Arrays.stream( entries )
				.map( v -> new ComboBoxEntry<>( v, v.toString() ) )
				.collect( Collectors.toList() );
		return comboBoxElement( label, get, set, list );
	}

	public static < T > ComboBoxElement< T > comboBoxElement( final String label,
			final Supplier< T > get, final Consumer< T > set,
			final List< ComboBoxEntry< T > > entries )
	{
		return new ComboBoxElement< T >( label, entries )
		{
			@Override
			public T get()
			{
				return get.get();
			}

			@Override
			public void set( final T t )
			{
				set.accept( t );
			}
		};
	}

	public static < T > ComboBoxEntry< T > cbentry( final T value, final String name )
	{
		return new ComboBoxEntry<>( value, name );
	}

	public interface StyleElementVisitor
	{
		default void visit( final Separator element )
		{
			throw new UnsupportedOperationException();
		}

		default void visit( final ColorElement colorElement )
		{
			throw new UnsupportedOperationException();
		}

		default void visit( final BooleanElement booleanElement )
		{
			throw new UnsupportedOperationException();
		}

		default void visit( final ComboBoxElement<?> comboBoxElement )
		{
			throw new UnsupportedOperationException();
		}
	}

	/*
	 *
	 * ===============================================================
	 *
	 */

	public interface StyleElement
	{
		default void update()
		{
		}

		void accept( StyleElementVisitor visitor );
	}

	public static class Separator implements StyleElement
	{
		@Override
		public void accept( final StyleElementVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	public static abstract class ColorElement implements StyleElement
	{
		private final ArrayList< IntConsumer > onSet = new ArrayList<>();

		private final String label;

		public ColorElement( final String label )
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}

		@Override
		public void accept( final StyleElementVisitor visitor )
		{
			visitor.visit( this );
		}

		public void onSet( final IntConsumer set )
		{
			onSet.add( set );
		}

		@Override
		public void update()
		{
			onSet.forEach( c -> c.accept( getColor() ) );
		}

		public abstract int getColor();

		public abstract void setColor( int c );
	}

	public static abstract class BooleanElement implements StyleElement
	{
		private final String label;

		private final ArrayList< Consumer< Boolean > > onSet = new ArrayList<>();

		public BooleanElement( final String label )
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}

		@Override
		public void accept( final StyleElementVisitor visitor )
		{
			visitor.visit( this );
		}

		public void onSet( final Consumer< Boolean > set )
		{
			onSet.add( set );
		}

		@Override
		public void update()
		{
			onSet.forEach( c -> c.accept( get() ) );
		}

		public abstract boolean get();

		public abstract void set( boolean b );
	}

	static class ComboBoxEntry< T >
	{
		private final T value;

		private final String name;

		ComboBoxEntry( final T value, final String name )
		{
			this.value = value;
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public T value()
		{
			return value;
		}
	}

	public static abstract class ComboBoxElement< T > implements StyleElement
	{
		private final String label;

		private final List< ComboBoxEntry< T > > entries;

		private final ArrayList< Consumer< T > > onSet = new ArrayList<>();

		public ComboBoxElement( final String label, final List< ComboBoxEntry< T > > entries )
		{
			this.label = label;
			this.entries = entries;
			if ( entries.isEmpty() )
				throw new IllegalArgumentException();
		}

		public String getLabel()
		{
			return label;
		}

		@Override
		public void accept( final StyleElementVisitor visitor )
		{
			visitor.visit( this );
		}

		public void onSet( final Consumer< T > set )
		{
			onSet.add( set );
		}

		@Override
		public void update()
		{
			onSet.forEach( c -> c.accept( get() ) );
		}

		public abstract T get();

		public abstract void set( T t );

		public List< ComboBoxEntry< T > > entries()
		{
			return entries;
		}
	}

	/*
	 *
	 * ===============================================================
	 *
	 */

	public static JCheckBox linkedCheckBox( final BooleanElement element, final String label )
	{
		final JCheckBox checkbox = new JCheckBox( label, element.get() );
		checkbox.setFocusable( false );
		checkbox.addActionListener( ( e ) -> element.set( checkbox.isSelected() ) );
		element.onSet( b -> {
			if ( b != checkbox.isSelected() )
				checkbox.setSelected( b );
		} );
		return checkbox;
	}

	public static JCheckBox linkedCheckBox( final BooleanElement element )
	{
		final JCheckBox checkbox = new JCheckBox();
		checkbox.setSelected( element.get() );
		checkbox.setFocusable( false );
		checkbox.addActionListener( ( e ) -> element.set( checkbox.isSelected() ) );
		element.onSet( b -> {
			if ( b != checkbox.isSelected() )
				checkbox.setSelected( b );
		} );
		return checkbox;
	}

	private static Color asColor( final int rgba )
	{
		return new Color( rgba, true );
	}

	private static int asInt( final Color color )
	{
		return color.getRGB();
	}

	public static JButton linkedColorButton( final ColorElement element, final JColorChooser colorChooser )
	{
		final Color outlineColor = Color.BLACK;
		final ColorIcon icon = new ColorIcon( asColor( element.getColor() ), 13, 13, 5, 5, true,outlineColor );
		final JButton button = new JButton( icon );
		button.setOpaque( false );
		button.setContentAreaFilled( false );
		button.setBorderPainted( false );
		button.setMargin( new Insets( 0, 0, 0, 0 ) );
		button.setBorder( new EmptyBorder( 0, 0, 1, 1 ) );
		button.setHorizontalAlignment( SwingConstants.LEFT );
		button.setFocusable( false );
		button.addActionListener( e -> {
			colorChooser.setColor( asColor( element.getColor() ) );
			final JDialog d = JColorChooser.createDialog( button, "Choose a color", true, colorChooser, new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent arg0 )
				{
					final Color c = colorChooser.getColor();
					if ( c != null )
					{
						icon.setColor( c );
						button.repaint();
						element.setColor( asInt( c ) );
					}
				}
			}, null );
			d.setVisible( true );
		} );
		element.onSet( rgba -> icon.setColor( asColor( rgba ) ) );
		return button;
	}

	public static < T > JComboBox< ComboBoxEntry< T > > linkedComboBox( final ComboBoxElement< T > element )
	{
		Vector< ComboBoxEntry< T > > vector = new Vector<>();
		vector.addAll( element.entries() );
		final JComboBox< ComboBoxEntry< T > > comboBox = new JComboBox<>( vector );
		comboBox.setEditable( false );
		comboBox.addItemListener( e -> {
			if ( e.getStateChange() == ItemEvent.SELECTED )
			{
				final T value = ( ( ComboBoxEntry< T > ) e.getItem() ).value();
				element.set( value );
			}
		} );
		final Consumer< T > setEntryForValue = value -> {
			for ( ComboBoxEntry< T > entry : vector )
				if ( Objects.equals( entry.value(), value ) )
				{
					comboBox.setSelectedItem( entry );
					break;
				}
		};
		element.onSet( setEntryForValue );
		setEntryForValue.accept( element.get() );
		return comboBox;
	}
}
