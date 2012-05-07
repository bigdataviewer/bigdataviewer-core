/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package mpicbg.tracking.transform;

import mpicbg.models.InvertibleCoordinateTransform;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AffineModel3D extends mpicbg.models.AffineModel3D implements InvertibleCoordinateTransform, CoordinateTransform
{
	public <P extends RealLocalizable & RealPositionable > void applyInPlace( final P p )
	{
		assert p.numDimensions() == 3 : "3d affine transformations can be applied to 3d points only.";

		final double l0 = p.getDoublePosition( 0 );
		final double l1 = p.getDoublePosition( 1 );
		final double l2 = p.getDoublePosition( 2 );
		p.setPosition( l0 * m00 + l1 * m01 + l2 * m02 + m03, 0 );
		p.setPosition( l0 * m10 + l1 * m11 + l2 * m12 + m13, 1 );
		p.setPosition( l0 * m20 + l1 * m21 + l2 * m22 + m23, 2 );
	}

	@Override
	final public void init( final String data )
	{
		final String[] fields = data.split( "\\s+" );
		if ( fields.length == 12 )
		{
			final float m00 = Float.parseFloat( fields[ 0 ] );
			final float m01 = Float.parseFloat( fields[ 1 ] );
			final float m02 = Float.parseFloat( fields[ 2 ] );
			final float m03 = Float.parseFloat( fields[ 3 ] );

			final float m10 = Float.parseFloat( fields[ 4 ] );
			final float m11 = Float.parseFloat( fields[ 5 ] );
			final float m12 = Float.parseFloat( fields[ 6 ] );
			final float m13 = Float.parseFloat( fields[ 7 ] );

			final float m20 = Float.parseFloat( fields[ 8 ] );
			final float m21 = Float.parseFloat( fields[ 9 ] );
			final float m22 = Float.parseFloat( fields[ 10 ] );
			final float m23 = Float.parseFloat( fields[ 11 ] );

			set(
					m00, m01, m02, m03,
					m10, m11, m12, m13,
					m20, m21, m22, m23 );
		}
		else throw new NumberFormatException( "Inappropriate parameters for " + this.getClass().getCanonicalName() );
	}

	@Override
	final public String toXML( final String indent )
	{
		return indent + "<iict_transform class=\"" + this.getClass().getCanonicalName() + "\" data=\"" + toDataString() + "\" />";
	}

	@Override
	final public String toDataString()
	{
		return
				m00 + " " + m01 + " " + m02 + " " + m03 + " " +
				m10 + " " + m11 + " " + m12 + " " + m13 + " " +
				m20 + " " + m21 + " " + m22 + " " + m23;
	}

	@Override
	public AffineModel3D copy()
	{
		final AffineModel3D m = new AffineModel3D();
		m.m00 = m00;
		m.m10 = m10;
		m.m20 = m20;
		m.m01 = m01;
		m.m11 = m11;
		m.m21 = m21;
		m.m02 = m02;
		m.m12 = m12;
		m.m22 = m22;
		m.m03 = m03;
		m.m13 = m13;
		m.m23 = m23;

		m.cost = cost;

		m.invert();

		return m;
	}

	@Override
	public AffineModel3D createInverse()
	{
		final AffineModel3D ict = new AffineModel3D();

		ict.m00 = i00;
		ict.m10 = i10;
		ict.m20 = i20;
		ict.m01 = i01;
		ict.m11 = i11;
		ict.m21 = i21;
		ict.m02 = i02;
		ict.m12 = i12;
		ict.m22 = i22;
		ict.m03 = i03;
		ict.m13 = i13;
		ict.m23 = i23;

		ict.i00 = m00;
		ict.i10 = m10;
		ict.i20 = m20;
		ict.i01 = m01;
		ict.i11 = m11;
		ict.i21 = m21;
		ict.i02 = m02;
		ict.i12 = m12;
		ict.i22 = m22;
		ict.i03 = m03;
		ict.i13 = m13;
		ict.i23 = m23;

		ict.cost = cost;

		return ict;
	}

	public static Element toXml( final Document doc, final AffineModel3D model )
	{
		final Element elem = doc.createElement( "iict_transform" );
		elem.setAttribute( "class", model.getClass().getCanonicalName() );
		elem.setAttribute( "data", model.toDataString() );
		return elem;
	}

	public static AffineModel3D fromXml( final Element iict_transform )
	{
		final AffineModel3D m = new AffineModel3D();
		m.init( iict_transform.getAttribute( "data" ) );
		return m;
	}
}
