/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package bdv.img.n5;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;

import java.io.IOException;

public class N5S3ImageLoader extends N5ImageLoader
{
	private final String serviceEndpoint;
	private final String signingRegion;
	private final String bucketName;
	private final String key;
	private final N5AmazonS3ReaderCreator.Authentication authentication;

	static class N5AmazonS3ReaderCreator
	{
		/**
		 * It seems that the way S3 works is that when a user has no credentials it means anonymous,
		 * but as soon as you provide some credentials it tries to get access with those,
		 * which indeed don't have access for that specific bucket.
		 * So it seems the way to go is to define in the application whether
		 * you want to use anonymous access or credentials based access
		 */
		public enum Authentication
		{
			Anonymous,
			Protected
		}

		public N5AmazonS3Reader create( String serviceEndpoint, String signingRegion, String bucketName, String key, Authentication authentication ) throws IOException
		{
			final AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration( serviceEndpoint, signingRegion );

			final AmazonS3 s3 = AmazonS3ClientBuilder
					.standard()
					.withPathStyleAccessEnabled( true )
					.withEndpointConfiguration( endpoint )
					.withCredentials( getCredentialsProvider( authentication ) )
					.build();

			return new N5AmazonS3Reader( s3, bucketName, key );
		}

		private AWSCredentialsProvider getCredentialsProvider( Authentication authentication )
		{
			switch ( authentication )
			{
				case Anonymous:
					return new AWSStaticCredentialsProvider( new AnonymousAWSCredentials() );
				case Protected:
					final DefaultAWSCredentialsProviderChain credentialsProvider = new DefaultAWSCredentialsProviderChain();
					checkCredentialsExistence( credentialsProvider );
					return credentialsProvider;
				default:
					throw new UnsupportedOperationException( "Authentication not supported: " + authentication );
			}
		}

		private void checkCredentialsExistence( AWSCredentialsProvider credentialsProvider )
		{
			try
			{
				credentialsProvider.getCredentials();
			}
			catch ( Exception e )
			{
				throw  new RuntimeException( e ); // No credentials could be found
			}
		}
	}

	public N5S3ImageLoader( String serviceEndpoint, String signingRegion, String bucketName, String key, N5AmazonS3ReaderCreator.Authentication authentication, AbstractSequenceDescription< ?, ?, ? > sequenceDescription ) throws IOException
	{
		super( new N5AmazonS3ReaderCreator().create( serviceEndpoint, signingRegion, bucketName, key, authentication ), sequenceDescription );
		this.serviceEndpoint = serviceEndpoint;
		this.signingRegion = signingRegion;
		this.bucketName = bucketName;
		this.key = key;
		this.authentication = authentication;
	}

	public String getServiceEndpoint()
	{
		return serviceEndpoint;
	}

	public String getSigningRegion()
	{
		return signingRegion;
	}

	public String getBucketName()
	{
		return bucketName;
	}

	public String getKey()
	{
		return key;
	}

	public N5AmazonS3ReaderCreator.Authentication getAuthentication()
	{
		return authentication;
	}
}
