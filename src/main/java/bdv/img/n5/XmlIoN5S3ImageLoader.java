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
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import org.janelia.saalfeldlab.n5.s3.N5AmazonS3Reader;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;

@ImgLoaderIo( format = "bdv.n5.s3", type = N5ImageLoader.class )
public class XmlIoN5S3ImageLoader implements XmlIoBasicImgLoader< N5GenericImageLoader >
{
	@Override
	public Element toXml( final N5GenericImageLoader imgLoader, final File basePath )
	{
		final Element elem = new Element( "ImageLoader" );
		// TODO
//		elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, "bdv.n5.s3" );
//		elem.setAttribute( "version", "1.0" );
//		elem.addContent( XmlHelpers.pathElement( "n5", imgLoader.getN5File(), basePath ) );
		return elem;
	}

	@Override
	public N5GenericImageLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		final String version = elem.getAttributeValue( "version" );

		final String serviceEndpoint = XmlHelpers.getText( elem, "ServiceEndpoint" );
		final String signingRegion = XmlHelpers.getText( elem, "SigningRegion" );
		final String bucketName = XmlHelpers.getText( elem, "BucketName" );
		final String key = XmlHelpers.getText( elem, "Key" );

		final AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration( serviceEndpoint, signingRegion );

		AWSCredentialsProvider credentialsProvider;
		try
		{
			final DefaultAWSCredentialsProviderChain defaultAWSCredentialsProviderChain = new DefaultAWSCredentialsProviderChain();
			// Below call throws error if there are no credentials
			defaultAWSCredentialsProviderChain.getCredentials();
			credentialsProvider = defaultAWSCredentialsProviderChain;
		}
		catch ( Exception e )
		{
			// User has no credentials on their computer
			credentialsProvider = new AWSStaticCredentialsProvider( new AnonymousAWSCredentials() );
		}

		final AmazonS3 s3 = AmazonS3ClientBuilder
				.standard()
				.withPathStyleAccessEnabled( true )
				.withEndpointConfiguration( endpoint )
				.withCredentials( credentialsProvider )
				.build();

		final N5AmazonS3Reader reader = getReader( s3, bucketName, key );

		return new N5GenericImageLoader( reader, sequenceDescription );
	}

	private N5AmazonS3Reader getReader( AmazonS3 s3, String bucketName, String key )
	{
		try
		{
			return new N5AmazonS3Reader( s3, bucketName, key );
		} catch ( IOException e )
		{
			e.printStackTrace();
		}
		return null;
	}
}
