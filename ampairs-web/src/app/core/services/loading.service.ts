import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  private loadingSubject = new BehaviorSubject<boolean>(false);
  private loadingCount = 0;

  public loading$ = this.loadingSubject.asObservable();

  setLoading(loading: boolean): void {
    if (loading) {
      this.loadingCount++;
    } else {
      this.loadingCount = Math.max(0, this.loadingCount - 1);
    }
    
    this.loadingSubject.next(this.loadingCount > 0);
  }

  isLoading(): boolean {
    return this.loadingSubject.value;
  }
}