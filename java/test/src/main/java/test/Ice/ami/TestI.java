// **********************************************************************
//
// Copyright (c) 2003-2014 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

package test.Ice.ami;

import test.Ice.ami.Test._TestIntfDisp;
import test.Ice.ami.Test.TestIntfException;

public class TestI extends _TestIntfDisp
{
    TestI()
    {
    }

    @Override
    public void
    op(Ice.Current current)
    {
    }

    @Override
    public int
    opWithResult(Ice.Current current)
    {
        return 15;
    }

    @Override
    public void
    opWithUE(Ice.Current current)
        throws TestIntfException
    {
        throw new TestIntfException();
    }

    @Override
    public void
    opWithPayload(byte[] seq, Ice.Current current)
    {
    }

    @Override
    public synchronized void
    opBatch(Ice.Current current)
    {
        ++_batchCount;
        notify();
    }

    @Override
    public synchronized int
    opBatchCount(Ice.Current current)
    {
        return _batchCount;
    }

    @Override
    public synchronized boolean
    waitForBatch(int count, Ice.Current current)
    {
        while(_batchCount < count)
        {
            try
            {
                wait(5000);
            }
            catch(InterruptedException ex)
            {
            }
        }
        boolean result = count == _batchCount;
        _batchCount = 0;
        return result;
    }

    @Override
    public void
    close(boolean force, Ice.Current current)
    {
        current.con.close(force);
    }

    @Override
    public void
    shutdown(Ice.Current current)
    {
        current.adapter.getCommunicator().shutdown();
    }

    private int _batchCount;
}