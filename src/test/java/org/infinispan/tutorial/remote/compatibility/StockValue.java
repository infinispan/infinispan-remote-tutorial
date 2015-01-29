package org.infinispan.tutorial.remote.compatibility;

import java.io.Serializable;
import java.util.Date;

/**
 * Stock value class. It must be Serializable, Externalizable, or somehow
 * marshallable in order to be transformed into different representations
 * depending on the endpoint used to access it.
 */
public final class StockValue implements Serializable {

   private final float value;
   private final Date date;

   public StockValue(float value) {
      this.value = value;
      this.date = new Date();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      StockValue that = (StockValue) o;

      if (Float.compare(that.value, value) != 0) return false;
      if (!date.equals(that.date)) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = (value != +0.0f ? Float.floatToIntBits(value) : 0);
      result = 31 * result + date.hashCode();
      return result;
   }

   @Override
   public String toString() {
      return "ShareState{" + "value=" + value + ", date=" + date + '}';
   }

}
