__constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

__kernel void slice1(
		__read_only image3d_t blocks,
		__write_only image2d_t target )
{
	const int4 pos = {
			get_global_id(0),
			get_global_id(1),
			8,
			0
	};

	const float min = 700;
	const float max = 3991;
	const float scale = 255.0 / (max - min + 1);
	const float offset = - min * scale;

	uint v = (uint) mad(read_imageui(blocks, sampler, pos).x, scale, offset);
	write_imageui(target, pos.xy, v);
}

__kernel void slice2(
		__global int4* blockLookup,
		__read_only image3d_t blocks,
		__write_only image2d_t target )
{
	const int3 gpos = {
			get_global_id(0),
			get_global_id(1),
			22
	};

	const int3 blockDimensions = { 64, 64, 16 };
	const int3 imageDimensions = { 958, 386, 44 };
	const int3 lookupDimensions = { 15, 7, 3 };

	const int3 lpos = gpos / blockDimensions;
	const int li = lpos.x + lpos.y * lookupDimensions.x + lpos.z * lookupDimensions.x * lookupDimensions.y;
	const int4 pos = blockLookup[ li ] + (int4)( (int3)fmod( (float3)gpos, (float3)blockDimensions ), 0 );

	const float min = 700;
	const float max = 3991;
	const float scale = 255.0 / (max - min + 1);
	const float offset = - min * scale;

	uint v = (uint) mad(read_imageui(blocks, sampler, pos).x, scale, offset);
	write_imageui(target, gpos.xy, v);
}

__kernel void slice3(
		__global int4* blockLookup,
		__read_only image3d_t blocks,
		__write_only image2d_t target )
{
	float f = 10000;
	for ( int z = 16; z < 32; ++z )
	{
		const int3 gpos = {
				get_global_id(0),
				get_global_id(1),
				z
		};

		const int3 blockDimensions = { 64, 64, 16 };
//		const int3 imageDimensions = { 958, 386, 44 };
		const int3 lookupDimensions = { 15, 7, 3 };

		const int3 lpos = gpos / blockDimensions;
		const int li = lpos.x + lpos.y * lookupDimensions.x + lpos.z * lookupDimensions.x * lookupDimensions.y;
		const int4 pos = blockLookup[ li ] + (int4)(gpos - lpos * blockDimensions, 0);

		f = fmin(f, read_imageui(blocks, sampler, pos).x);
	}
	const float min = 700;
	const float max = 3991;
	const float scale = 255.0 / (max - min + 1);
	const float offset = - min * scale;
	uint v = (uint) mad(f, scale, offset);
	write_imageui(target, (int2)(get_global_id(0),get_global_id(1)), v);
}



float3 affine( __constant float4* m, float4 p )
{
	return (float3) (dot(m[0], p), dot(m[1], p), dot(m[2], p));
}


__kernel void slice4(
		__constant float4* transform,
		__constant uint4* sizes,
		__global __read_only int3* blockLookup,
		__read_only image3d_t blocks,
		__write_only image2d_t target )
{
	const int2 opos = {
			get_global_id(0),
			get_global_id(1)
	};
	const uint3 blockDimensions = sizes[ 0 ].xyz;
	const uint3 lookupDimensions = sizes[ 1 ].xyz;

	float f = 0;
	for ( int z = 0; z < 100; ++z )
	{
		const float4 gpos = {
				get_global_id(0),
				get_global_id(1),
				z,
				1
		};
		float3 p = affine( transform, gpos );
		int3 pr = convert_int3( round( p ) );
		int3 prb = convert_int3( floor( round( p ) / convert_float3( blockDimensions ) ) );
		if ( ! ( any( prb < (int3) (0, 0, 0) ) || any( prb >= lookupDimensions ) ) )
		{
			uint li = prb.x + prb.y * lookupDimensions.x + prb.z * lookupDimensions.x * lookupDimensions.y;
			int4 pos = (int4) ( blockLookup[ li ] + pr - prb * blockDimensions, 0 );
			f = fmax(f, read_imageui(blocks, sampler, pos).x);
//			f = 100;
		}
	}
	const float min = 700;
	const float max = 3991;
	const float scale = 255.0 / (max - min + 1);
	const float offset = - min * scale;
	uint v = (uint) mad(f, scale, offset);
	write_imageui( target, opos, v );
}

