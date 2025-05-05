/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.scijava.links.AbstractLinkHandler;
import org.scijava.links.LinkHandler;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParseException;

import javax.swing.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Plugin(type = LinkHandler.class)
public class BDVLinkHandlerPlugin extends AbstractLinkHandler {

    private static final Logger LOG = LoggerFactory.getLogger( LinkActions.class );

    public static final String PLUGIN_NAME = "BDV";

    @Parameter
    private UIService uiService;

    @Override
    public void handle(final URI uri){
        if (!supports(uri)) throw new UnsupportedOperationException("" + uri);
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            // TODO open an empty BDV window
            throw new UnsupportedOperationException("Not implemented yet");
        } else {
            String decoded_query;
            try{
                decoded_query = URLDecoder.decode(query, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                LOG.error("Failed to decode JSON from URI: {}", uri, e);
                return;
            }
            try
            {
                // TODO handle the decoded JSON query
                // Links.paste( decoded_query, panel, converterSetups, pasteSettings, resources );
                throw new UnsupportedOperationException("Not implemented yet");
            }
            catch ( final JsonParseException | IllegalArgumentException e )
            {
                LOG.debug( "pasted JSON is malformed:\n\"{}\"", pastedText, e );
            }   
            }

    }



  @Override
    public boolean supports(final URI uri) {
        return super.supports(uri) && PLUGIN_NAME.equals(org.scijava.links.Links.operation(uri));
    }


}