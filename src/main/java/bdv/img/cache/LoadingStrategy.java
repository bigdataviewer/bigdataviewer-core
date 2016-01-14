/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
package bdv.img.cache;

import bdv.img.cache.CacheIoTiming.IoTimeBudget;

/**
 * Describes how the cache processes requests for cells with missing data.
 *
 * Depending on the {@link LoadingStrategy} the following actions are performed
 * if the cell data has not been loaded yet:
 * <ul>
 *   <li> {@link LoadingStrategy#VOLATILE}:
 *        Enqueue the cell for asynchronous loading by a fetcher thread.
 *   <li> {@link LoadingStrategy#BLOCKING}:
 *        Load the cell data immediately.
 *   <li> {@link LoadingStrategy#BUDGETED}:
 *        Load the cell data immediately if there is enough {@link IoTimeBudget}
 *        left for the current thread group. Otherwise enqueue the cell for
 *        asynchronous loading by a fetcher thread.
 *   <li> {@link LoadingStrategy#DONTLOAD}:
 *        Do nothing.
 * </ul>
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public enum LoadingStrategy
{
	VOLATILE,
	BLOCKING,
	BUDGETED,
	DONTLOAD
}
