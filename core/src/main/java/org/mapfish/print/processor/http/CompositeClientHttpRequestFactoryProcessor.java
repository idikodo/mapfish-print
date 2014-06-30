/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.processor.http;

import com.google.common.collect.Lists;
import org.mapfish.print.output.Values;
import org.mapfish.print.processor.AbstractProcessor;
import org.mapfish.print.processor.ProcessorUtils;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.util.List;
import javax.annotation.Nullable;

/**
 * A processor that wraps several {@link AbstractClientHttpRequestFactoryProcessor}s.
 * <p/>
 * This makes it more convenient to configure multiple processors that modify
 * {@link org.springframework.http.client.ClientHttpRequestFactory} objects.
 *
 * @author Jesse on 6/25/2014.
 */
public final class CompositeClientHttpRequestFactoryProcessor
        extends AbstractProcessor<Values, ClientHttpFactoryProcessorParam>
        implements HttpProcessor<Values> {
    private List<HttpProcessor> parts = Lists.newArrayList();

    /**
     * Constructor.
     */
    protected CompositeClientHttpRequestFactoryProcessor() {
        super(ClientHttpFactoryProcessorParam.class);
    }

    public void setParts(final List<HttpProcessor> parts) {
        this.parts = parts;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ClientHttpRequestFactory createFactoryWrapper(final Values values,
                                                         final ClientHttpRequestFactory requestFactory) {
        ClientHttpRequestFactory finalRequestFactory = requestFactory;
        // apply the parts in reverse so that the last part is the inner most wrapper (will be last to be called)
        for (int i = this.parts.size() - 1; i > -1; i--) {
            final HttpProcessor processor = this.parts.get(i);
            Object input = ProcessorUtils.populateInputParameter(processor, values);
            finalRequestFactory = processor.createFactoryWrapper(input, finalRequestFactory);
        }
        return finalRequestFactory;
    }

    @Override
    protected void extraValidation(final List<Throwable> validationErrors) {
        if (this.parts.isEmpty()) {
            validationErrors.add(new IllegalStateException("There are no composite elements for this processor"));
        } else {
            for (Object part : this.parts) {
                if (!(part instanceof HttpProcessor)) {
                    validationErrors.add(new IllegalStateException("One of the parts of " + getClass().getSimpleName() + " is not a " +
                                                                   HttpProcessor.class.getSimpleName()));
                }
            }
        }
    }

    @Nullable
    @Override
    public Values createInputParameter() {
        return new Values();
    }

    @Nullable
    @Override
    public ClientHttpFactoryProcessorParam execute(final Values values,
                                                   final ExecutionContext context) throws Exception {
        ClientHttpRequestFactory requestFactory = values.getObject(Values.CLIENT_HTTP_REQUEST_FACTORY_KEY,
                ClientHttpRequestFactory.class);

        final ClientHttpFactoryProcessorParam output = new ClientHttpFactoryProcessorParam();
        output.clientHttpRequestFactory = createFactoryWrapper(values, requestFactory);
        return output;
    }
}
