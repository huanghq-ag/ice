// **********************************************************************
//
// Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

package Freeze;

public abstract class Index implements com.sleepycat.db.SecondaryKeyCreator
{
    //
    // Implementation details
    //
    
    public boolean
    createSecondaryKey(com.sleepycat.db.SecondaryDatabase secondary,
                       com.sleepycat.db.DatabaseEntry key,
                       com.sleepycat.db.DatabaseEntry value,
                       com.sleepycat.db.DatabaseEntry result)
        throws com.sleepycat.db.DatabaseException
    {
        Ice.Communicator communicator = _store.communicator();
        ObjectRecord rec = ObjectStore.unmarshalValue(value.getData(), communicator);

        byte[] secondaryKey = marshalKey(rec.servant);
        if(secondaryKey != null)
        {
            result.setData(secondaryKey);
            result.setSize(secondaryKey.length);
            return true;
        }
        else
        {
            //
            // Don't want to index this one
            //
            return false;
        }
    }

    public String
    name()
    {
        return _name;
    }

    public String
    facet()
    {
        return _facet;
    }

    protected Index(String name, String facet)
    {
        _name = name;
        _facet = facet;
    }

    protected abstract byte[]
    marshalKey(Ice.Object servant);
    
    protected Ice.Identity[]
    untypedFindFirst(byte[] k, int firstN)
    {
        EvictorI.DeactivateController deactivateController = _store.evictor().deactivateController();
        deactivateController.lock();
        try
        {
            com.sleepycat.db.DatabaseEntry key = new com.sleepycat.db.DatabaseEntry(k);
        
            // When we have a custom-comparison function, Berkeley DB returns
            // the key on-disk (when it finds one). We disable this behavior:
            // (ref Oracle SR 5925672.992)
            //
            key.setPartial(true);
            
            com.sleepycat.db.DatabaseEntry pkey = new com.sleepycat.db.DatabaseEntry();
            com.sleepycat.db.DatabaseEntry value = new com.sleepycat.db.DatabaseEntry();
            //
            // dlen is 0, so we should not retrieve any value 
            // 
            value.setPartial(true);
            
            Ice.Communicator communicator = _store.communicator();

            TransactionI transaction = _store.evictor().beforeQuery();
            com.sleepycat.db.Transaction tx = transaction == null ? null : transaction.dbTxn();

            java.util.List identities;
        
            for(;;)
            {
                com.sleepycat.db.SecondaryCursor dbc = null;
                identities = new java.util.ArrayList(); 
            
                try
                {
                    //
                    // Move to the first record
                    // 
                    dbc = _db.openSecondaryCursor(tx, null);
                    boolean first = true;
                
                    boolean found;
                
                    do
                    {
                        com.sleepycat.db.OperationStatus status;
                        if(first)
                        {
                            status = dbc.getSearchKey(key, pkey, value, null);
                        }
                        else
                        {
                            status = dbc.getNextDup(key, pkey, value, null);
                        }
                    
                        found = status == com.sleepycat.db.OperationStatus.SUCCESS;
                    
                        if(found)
                        {
                            Ice.Identity ident = ObjectStore.unmarshalKey(pkey.getData(), communicator);
                            identities.add(ident);
                            first = false;
                        }
                    }
                    while((firstN <= 0 || identities.size() < firstN) && found);
                    
                    break; // for(;;)
                }
                catch(com.sleepycat.db.DeadlockException dx)
                {
                    if(_store.evictor().deadlockWarning())
                    {
                        communicator.getLogger().warning("Deadlock in Freeze.Index.untypedFindFirst while " +
                                                         "iterating over Db \"" + _store.evictor().filename() +
                                                         "/" + _dbName + "\"");
                    }
                
                    if(tx != null)
                    {
                        DeadlockException ex = new DeadlockException();
                        ex.initCause(dx);
                        ex.message = _store.evictor().errorPrefix() + "Db.cursor: " + dx.getMessage();
                        throw ex;
                    }
                
                    //
                    // Otherwise retry
                    //
                }
                catch(com.sleepycat.db.DatabaseException dx)
                {
                    DatabaseException ex = new DatabaseException();
                    ex.initCause(dx);
                    ex.message = _store.evictor().errorPrefix() + "Db.cursor: " + dx.getMessage();
                    throw ex;
                }
                finally
                {
                    if(dbc != null)
                    {
                        try
                        {
                            dbc.close();
                        }
                        catch(com.sleepycat.db.DeadlockException dx)
                        {
                            if(tx != null)
                            {
                                DeadlockException ex = new DeadlockException();
                                ex.initCause(dx);
                                ex.message = _store.evictor().errorPrefix() + "Db.cursor: " + dx.getMessage();
                                throw ex;
                            }
                        }
                        catch(com.sleepycat.db.DatabaseException dx)
                        {
                            //
                            // Ignored
                            //
                        }
                    }
                }
            }
           
            if(identities.size() != 0)
            {
                Ice.Identity[] result = new Ice.Identity[identities.size()];
                return (Ice.Identity[])identities.toArray(result);
            }
            else
            {
                return new Ice.Identity[0];
            }
        }
        finally
        {
            deactivateController.unlock();
        }
    }
        
    protected Ice.Identity[]
    untypedFind(byte[] key)
    {
        return untypedFindFirst(key, 0);
    }

    protected int
    untypedCount(byte[] k)
    {
        EvictorI.DeactivateController deactivateController = _store.evictor().deactivateController();
        deactivateController.lock();
        try
        {

            com.sleepycat.db.DatabaseEntry key = new com.sleepycat.db.DatabaseEntry(k);
        
            // When we have a custom-comparison function, Berkeley DB returns
            // the key on-disk (when it finds one). We disable this behavior:
            // (ref Oracle SR 5925672.992)
            //
            key.setPartial(true);
        
            com.sleepycat.db.DatabaseEntry value = new com.sleepycat.db.DatabaseEntry();
            //
            // dlen is 0, so we should not retrieve any value 
            // 
            value.setPartial(true);
            TransactionI transaction = _store.evictor().beforeQuery();
            com.sleepycat.db.Transaction tx = transaction == null ? null : transaction.dbTxn();
        
            for(;;)
            {
                com.sleepycat.db.Cursor dbc = null;
                try
                {
                    dbc = _db.openCursor(tx, null);   
                    if(dbc.getSearchKey(key, value, null) == com.sleepycat.db.OperationStatus.SUCCESS)
                    {
                        return dbc.count();
                    }
                    else
                    {
                        return 0;
                    }
                }
                catch(com.sleepycat.db.DeadlockException dx)
                {
                    if(_store.evictor().deadlockWarning())
                    {
                        _store.communicator().getLogger().warning("Deadlock in Freeze.Index.untypedCount while " +
                                                                  "iterating over Db \"" +
                                                                  _store.evictor().filename() + "/" + _dbName +
                                                                  "\"");
                    }

                    if(tx != null)
                    {
                        DeadlockException ex = new DeadlockException();
                        ex.initCause(dx);
                        ex.message = _store.evictor().errorPrefix() + "Db.cursor: " + dx.getMessage();
                        throw ex;
                    }
                    //
                    // Otherwise retry
                    //
                }
                catch(com.sleepycat.db.DatabaseException dx)
                {
                    DatabaseException ex = new DatabaseException();
                    ex.initCause(dx);
                    ex.message = _store.evictor().errorPrefix() + "Db.cursor: " + dx.getMessage();
                    throw ex;
                }
                finally
                {
                    if(dbc != null)
                    {
                        try
                        {
                            dbc.close();
                        }
                        catch(com.sleepycat.db.DeadlockException dx)
                        {
                            if(tx != null)
                            {
                                DeadlockException ex = new DeadlockException();
                                ex.initCause(dx);
                                ex.message = _store.evictor().errorPrefix() + "Db.cursor: " + dx.getMessage();
                                throw ex;
                            }
                        }
                        catch(com.sleepycat.db.DatabaseException dx)
                        {
                            //
                            // Ignored
                            //
                        }
                    }
                }
            }
        }
        finally
        {
            deactivateController.unlock();
        }
    }
 
    protected final Ice.Communicator
    communicator()
    {
        return _store.communicator();
    }
    
    void
    associate(ObjectStore store, com.sleepycat.db.Transaction txn, boolean createDb, boolean populateIndex)
        throws com.sleepycat.db.DatabaseException, java.io.FileNotFoundException
    {
        assert(txn != null);
        _store = store;
        
        _dbName = EvictorI.indexPrefix + store.dbName() + "." + _name;

        com.sleepycat.db.SecondaryConfig config = new com.sleepycat.db.SecondaryConfig();
        config.setAllowCreate(createDb);
        config.setAllowPopulate(populateIndex);
        config.setSortedDuplicates(true);
        config.setType(com.sleepycat.db.DatabaseType.BTREE);
        config.setKeyCreator(this);

        _db = _store.evictor().dbEnv().getEnv().openSecondaryDatabase(txn, _store.evictor().filename(), _dbName,
                                                                      _store.db(), config);
    }

    void
    close()
    {
        if(_db != null)
        {
            try
            {
                _db.close();
            }
            catch(com.sleepycat.db.DatabaseException dx)
            {
                DatabaseException ex = new DatabaseException();
                ex.initCause(dx);
                ex.message = _store.evictor().errorPrefix() + "Db.close: " + dx.getMessage();
                throw ex;
            }
            _db = null;   
        }
    }
  
    private final String _name;
    private final String _facet;
    private String _dbName;

    private com.sleepycat.db.SecondaryDatabase _db = null;
    private ObjectStore _store = null;
}
