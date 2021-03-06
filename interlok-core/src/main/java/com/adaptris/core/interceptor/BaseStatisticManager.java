package com.adaptris.core.interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.core.CoreException;

public abstract class BaseStatisticManager implements StatisticManager {
  
  protected transient Logger log = LoggerFactory.getLogger(this.getClass());
  
  private transient List<InterceptorStatistic> stats;
    
  private transient Integer maxHistoryCount;
  
  public BaseStatisticManager() {
  }
  
  public BaseStatisticManager(int maxHistoryCount) {
    this.setMaxHistoryCount(maxHistoryCount);
    this.setStats(new MaxCapacityList<InterceptorStatistic>(maxHistoryCount));
  }

  public void setStats(List<InterceptorStatistic> stats) {
    this.stats = stats;
  }

  @Override
  public void clear() {
    this.stats.clear();
  }

  @Override
  public void updateCurrent(InterceptorStatistic currentTimeSlice) {
    this.stats().set(this.stats().size() - 1, currentTimeSlice);
  }

  @Override
  public InterceptorStatistic getLatestStat() {
    if (this.stats().size() == 0) {
      return null;
    }
    else {
      return this.stats().get(this.stats().size() - 1);
    }
  }

  @Override
  public List<InterceptorStatistic> getStats() {
    return this.stats();
  }
  
  protected List<InterceptorStatistic> stats() {
    if(this.stats != null)
      return this.stats;
    else
      this.setStats(new MaxCapacityList<InterceptorStatistic>(this.getMaxHistoryCount()));
    
    return this.stats;
  }

  public int getMaxHistoryCount() {
    return maxHistoryCount;
  }

  public void setMaxHistoryCount(int maxHistoryCount) {
    this.maxHistoryCount = maxHistoryCount;
  }
  
  @Override
  public void init() throws CoreException {
  }

  @Override
  public void start() throws CoreException {
  }

  @Override
  public void stop() {
  }

  @Override
  public void close() {
  }

  // We only use the add method internally, so we can safely do a throw for all
  // the others.
  protected class MaxCapacityList<E> extends ArrayList<E> {

    private static final long serialVersionUID = -7833969761130499160L;
    
    private int maxCapacity;
    
    public MaxCapacityList(int maxCapacity) {
      this.setMaxCapacity(maxCapacity);
    }

    public boolean add(E item) {
      while (size() >= this.getMaxCapacity()) {
        remove(0);
      }
      return super.add(item);
    }

    public void add(int index, E element) {
      throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends E> c) {
      throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection<? extends E> c) {
      throw new UnsupportedOperationException();
    }

    public int getMaxCapacity() {
      return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
      this.maxCapacity = maxCapacity;
    }
  }
}
