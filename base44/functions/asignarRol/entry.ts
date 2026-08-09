import { createClientFromRequest } from 'npm:@base44/sdk@0.8.38';

Deno.serve(async (req) => {
  try {
    const base44 = createClientFromRequest(req);
    const user = await base44.auth.me();
    if (!user) return Response.json({ error: 'Unauthorized' }, { status: 401 });

    const body = await req.json();
    const { role } = body;

    // Only allow non-admin roles
    const allowedRoles = ['chofer', 'usuario'];
    if (!allowedRoles.includes(role)) {
      return Response.json({ error: 'No tenés permiso para asignar ese rol' }, { status: 403 });
    }

    // Update the user's role using service role (elevated privileges)
    await base44.asServiceRole.entities.User.update(user.id, { role });

    return Response.json({ success: true, role });
  } catch (error) {
    return Response.json({ error: error.message }, { status: 500 });
  }
});