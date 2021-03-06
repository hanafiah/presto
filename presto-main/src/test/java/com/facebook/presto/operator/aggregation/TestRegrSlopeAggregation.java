/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.operator.aggregation;

import com.facebook.presto.spi.block.Block;
import com.facebook.presto.spi.type.StandardTypes;
import com.google.common.collect.ImmutableList;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.testng.annotations.Test;

import java.util.List;

import static com.facebook.presto.operator.aggregation.AggregationTestUtils.createDoubleArbitraryBlock;
import static com.facebook.presto.operator.aggregation.AggregationTestUtils.createDoubleSequenceBlock;
import static com.google.common.base.Preconditions.checkArgument;

public class TestRegrSlopeAggregation
        extends AbstractTestAggregationFunction
{
    @Override
    public Block[] getSequenceBlocks(int start, int length)
    {
        return new Block[] {createDoubleSequenceBlock(start, length), createDoubleSequenceBlock(start + 2, length)};
    }

    @Override
    protected String getFunctionName()
    {
        return "regr_slope";
    }

    @Override
    protected List<String> getFunctionParameterTypes()
    {
        return ImmutableList.of(StandardTypes.DOUBLE, StandardTypes.DOUBLE);
    }

    @Override
    public Object getExpectedValue(int start, int length)
    {
        if (length <= 1) {
            return null;
        }
        SimpleRegression regression = new SimpleRegression();
        for (int i = start; i < start + length; i++) {
            regression.addData(i + 2, i);
        }
        return regression.getSlope();
    }

    @Test
    public void testNonTrivialResult()
    {
        testNonTrivialAggregation(new double[] {1, 2, 3, 4, 5}, new double[] {1, 4, 9, 16, 25});
        testNonTrivialAggregation(new double[] {1, 4, 9, 16, 25}, new double[] {1, 2, 3, 4, 5});
    }

    private void testNonTrivialAggregation(double[] y, double[] x)
    {
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < x.length; i++) {
            regression.addData(x[i], y[i]);
        }
        double expected = regression.getSlope();
        checkArgument(Double.isFinite(expected) && expected != 0.0, "Expected result is trivial");
        testAggregation(expected, createDoubleArbitraryBlock(y), createDoubleArbitraryBlock(x));
    }
}
