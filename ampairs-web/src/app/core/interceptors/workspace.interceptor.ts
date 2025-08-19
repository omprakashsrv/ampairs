import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Observable} from 'rxjs';

@Injectable()
export class WorkspaceInterceptor implements HttpInterceptor {

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Get the selected workspace ID from localStorage
    const workspaceId = localStorage.getItem('workspace_id');

    // Only add workspace header for API requests (not for external services like reCAPTCHA)
    const isApiRequest = request.url.includes('/api/') ||
      request.url.includes('/workspace/') ||
      request.url.includes('/customer/') ||
      request.url.includes('/product/') ||
      request.url.includes('/order/') ||
      request.url.includes('/invoice/');

    // Don't add workspace header for authentication endpoints or specific workspace endpoints
    const isAuthEndpoint = request.url.includes('/auth/');
    const isUserEndpoint = request.url.includes('/user/');
    const isWorkspaceListEndpoint = request.url.includes('/workspace/v1/list') ||
      request.url.includes('/workspace/v1/create') ||
      request.url.includes('/workspace/v1/check-slug');

    // Special case: workspace detail endpoints (GET /workspace/v1/{id}) should not have workspace header
    // as they are used to fetch workspace details before setting the workspace context
    const isWorkspaceDetailEndpoint = /\/workspace\/v1\/[A-Z0-9]+$/.test(request.url) && request.method === 'GET';

    if (isApiRequest && !isAuthEndpoint && !isUserEndpoint && !isWorkspaceListEndpoint && !isWorkspaceDetailEndpoint && workspaceId) {
      // Clone the request and add the workspace header
      const modifiedRequest = request.clone({
        setHeaders: {
          'X-Workspace': workspaceId
        }
      });

      return next.handle(modifiedRequest);
    }

    return next.handle(request);
  }
}
