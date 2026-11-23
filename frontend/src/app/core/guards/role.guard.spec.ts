import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, provideRouter } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';

describe('roleGuard', () => {
  let auth: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    auth = jasmine.createSpyObj('AuthService', ['getRole', 'isAuthenticated', 'getHomeRoute']);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
      ],
    });
    router = TestBed.inject(Router);
    auth.getHomeRoute.and.returnValue('/student/dashboard');
  });

  function runGuard(roles: string[] | undefined) {
    const route = { data: { roles } } as unknown as ActivatedRouteSnapshot;
    return TestBed.runInInjectionContext(() => roleGuard(route, {} as never));
  }

  it('allows matching role', () => {
    auth.getRole.and.returnValue('STUDENT');
    expect(runGuard(['STUDENT'])).toBe(true);
  });

  it('blocks wrong role', () => {
    auth.getRole.and.returnValue('STUDENT');
    auth.isAuthenticated.and.returnValue(true);
    const result = runGuard(['ADMIN']);
    expect(result?.toString()).toBe(router.createUrlTree(['/student/dashboard']).toString());
  });
});