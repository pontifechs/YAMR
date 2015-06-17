package ninja.dudley.yamr.db;

import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

/**
 * Created by mdudley on 6/16/15.
 */
public class DBProviderTest extends ProviderTestCase2<DBProvider>
{
    private static MockContentResolver resolver;

    public DBProviderTest()
    {
        super(DBProvider.class, "ninja.dudley.yamr.db.DBProvider");
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        setContext(getMockContext());
        resolver = this.getMockContentResolver();
    }
}