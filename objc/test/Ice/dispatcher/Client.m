// **********************************************************************
//
// Copyright (c) 2003-2015 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

#import <objc/Ice.h>
#import <TestCommon.h>
#import <DispatcherTest.h>
#include <dispatch/dispatch.h>
#ifdef ICE_OBJC_GC
#   import <Foundation/NSGarbageCollector.h>
#endif

static int
run(id<ICECommunicator> communicator)
{
    TestDispatcherTestIntfPrx* dispatcherAllTests(id<ICECommunicator>);
    TestDispatcherTestIntfPrx* dispatcher = dispatcherAllTests(communicator);
    [dispatcher shutdown];
    return EXIT_SUCCESS;
}

#if TARGET_OS_IPHONE
#  define main dispatcherClient
#endif

int
main(int argc, char* argv[])
{
    int status;
    @autoreleasepool
    {
        id<ICECommunicator> communicator = nil;

        @try
        {
            ICEInitializationData* initData = [ICEInitializationData initializationData];
            initData.properties = defaultClientProperties(&argc, argv);
            dispatch_queue_t queue = dispatch_queue_create("Dispatcher", DISPATCH_QUEUE_SERIAL);
            initData.dispatcher = ^(id<ICEDispatcherCall> call, id<ICEConnection> con) {
                dispatch_sync(queue, ^ { [call run]; });
            };
#if TARGET_OS_IPHONE
            initData.prefixTable__ = [NSDictionary dictionaryWithObjectsAndKeys:
                                      @"TestDispatcher", @"::Test", 
                                      nil];
#endif
            communicator = [ICEUtil createCommunicator:&argc argv:argv initData:initData];
            status = run(communicator);
            dispatch_release(queue);
        }
        @catch(ICEException* ex)
        {
            tprintf("%@\n", ex);
            status = EXIT_FAILURE;
        }
        @catch(TestFailedException* ex)
        {
            status = EXIT_FAILURE;
        }

        if(communicator)
        {
            @try
            {
                [communicator destroy];
            }
            @catch(ICEException* ex)
            {
                tprintf("%@\n", ex);
                status = EXIT_FAILURE;
            }
        }
    }
#ifdef ICE_OBJC_GC
    [[NSGarbageCollector defaultCollector] collectExhaustively];
#endif
    return status;
}