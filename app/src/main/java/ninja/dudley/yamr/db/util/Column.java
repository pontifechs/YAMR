package ninja.dudley.yamr.db.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ninja.dudley.yamr.db.DBHelper;

/**
 * Created by mdudley on 7/2/15.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column
{
    enum Type
    {
        Text,
        Integer,
        Real,
        Datetime
    }

    Type type() default Type.Text;

    String name();

    int version() default 1;
}
