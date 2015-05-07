inline
float3 affine( __constant float4* m, float4 p )
{
	return (float3) (dot(m[0], p), dot(m[1], p), dot(m[2], p));
}

// intersect ray with a box
// http://www.siggraph.org/education/materials/HyperGraph/raytrace/rtinter3.htm
inline
float2 intersectBox( float3 r_o, float3 r_d, float3 boxmax )
{
    // compute intersection of ray with all six bbox planes
    float3 invR = (float3) ( 1.0f, 1.0f, 1.0f ) / r_d;
    float3 tbot = invR * -r_o;
    float3 ttop = invR * ( boxmax - r_o );

    // re-order intersections to find smallest and largest on each axis
    float3 tmin = min( ttop, tbot );
    float3 tmax = max( ttop, tbot );

    // find the largest tmin and the smallest tmax
    float largest_tmin = max( max( tmin.x, tmin.y ), tmin.z );
    float smallest_tmax = min( min( tmax.x, tmax.y ), tmax.z );

    return (float2) ( largest_tmin, smallest_tmax );
}


__kernel void slice(
		__constant float4* transform,
		__constant uint4* sizes,
		__global __read_only short3* blockLookup,
		__read_only image3d_t blocks,
		__write_only image2d_t target )
{
	const float dimZ = 100;
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_LINEAR;

	const uint x = get_global_id( 0 );
	const uint y = get_global_id( 1 );

	const uint3 blockDimensions = sizes[ 0 ].xyz;
	const float3 bdf = convert_float3(blockDimensions);
	const uint3 lookupDimensions = sizes[ 1 ].xyz;

	const float2 ms = { lookupDimensions[ 0 ], lookupDimensions[ 0 ] * lookupDimensions[ 1 ] };

	const float3 pb = affine( transform, (float4) ( x, y, 0, 1 ) );
	const float3 db = { transform[ 0 ].z, transform[ 1 ].z, transform[ 2 ].z };

	float2 nearfar = intersectBox( pb, db, convert_float3( lookupDimensions ) );
	nearfar.x = fmax( nearfar.x, 0 );
	nearfar.y = fmin( nearfar.y, dimZ );

	if ( nearfar.x > nearfar.y )
	{
		write_imageui( target, (int2) ( x, y ), 255 );
		return;
	}

	uint numSteps = (uint) (nearfar.y - nearfar.x + 1);

	float f = 0;
//	float3 p = pb + nearfar.x * db;
	float3 p = pb + nearfar.x * db + (float3)(0.5, 0.5, 0.5) / bdf;
	for ( uint i = 0; i < numSteps; ++i )
	{
		float3 ib;
		float3 r = fract(p, &ib);
		int idx = ( int ) ( ib.x + dot( ms, ib.yz ) );
		float3 offset = convert_float3( blockLookup[ idx ] );
		float4 texp = (float4) ( r * bdf + offset + (float3) ( 0.5, 0.5, 0.5 ), 0 );
		f = fmax(f, read_imageui( blocks, sampler, texp ).x);
		p += db;
	}



	const float min = 700;
	const float max = 3991;
	const float scale = 255.0 / (max - min + 1);
	const float offset = - min * scale;
	uint v = (uint) mad(f, scale, offset);
	write_imageui( target, (int2) ( x, y ), v );
}

__kernel void slice_nn(
		__constant float4* transform,
		__constant uint4* sizes,
		__global __read_only short3* blockLookup,
		__read_only image3d_t blocks,
		__write_only image2d_t target )
{
	const float dimZ = 100;
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

	const uint x = get_global_id( 0 );
	const uint y = get_global_id( 1 );

	const uint3 blockDimensions = sizes[ 0 ].xyz;
	const float3 bdf = convert_float3(blockDimensions);
	const uint3 lookupDimensions = sizes[ 1 ].xyz;

	const float2 ms = { lookupDimensions[ 0 ], lookupDimensions[ 0 ] * lookupDimensions[ 1 ] };

	const float3 pb = affine( transform, (float4) ( x, y, 0, 1 ) );
	const float3 db = { transform[ 0 ].z, transform[ 1 ].z, transform[ 2 ].z };

	float2 nearfar = intersectBox( pb, db, convert_float3( lookupDimensions ) );
	nearfar.x = fmax( nearfar.x, 0 );
	nearfar.y = fmin( nearfar.y, dimZ );

	if ( nearfar.x > nearfar.y )
	{
		write_imageui( target, (int2) ( x, y ), 255 );
		return;
	}

	uint numSteps = (uint) (nearfar.y - nearfar.x + 1);

	float f = 0;
	float3 p = pb + nearfar.x * db + (float3)(0.5, 0.5, 0.5) / bdf;
	for ( uint i = 0; i < numSteps; ++i )
	{
		float3 ib;
		float3 r = fract(p, &ib);
		int idx = ( int ) ( ib.x + dot( ms, ib.yz ) );
		float3 offset = convert_float3( blockLookup[ idx ] );
		float4 texp = (float4) ( r * bdf + offset, 0 );
		f = fmax(f, read_imageui( blocks, sampler, texp ).x);
		p += db;
	}



	const float min = 700;
	const float max = 3991;
	const float scale = 255.0 / (max - min + 1);
	const float offset = - min * scale;
	uint v = (uint) mad(f, scale, offset);
	write_imageui( target, (int2) ( x, y ), v );
}

