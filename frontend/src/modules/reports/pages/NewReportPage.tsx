import { Card, CardContent, Grid, Typography } from '@mui/material';
import { PageHeader } from '../../../shared/components/PageHeader';
import { ReportForm } from '../components/ReportForm';
import { ReportTracker } from '../components/ReportTracker';

/** RF-10 · Denuncia desde la app, con la sesión iniciada (identificada o anónima, a elección). */
export function NewReportPage() {
  return (
    <>
      <PageHeader
        title="Denunciar pesca ilegal"
        subtitle="Reporta lo que viste en el mar o en el muelle. Si eliges denuncia anónima, no queda ningún vínculo con tu cuenta."
      />
      <Grid container spacing={2.5}>
        <Grid size={{ xs: 12, lg: 7 }}>
          <Card>
            <CardContent sx={{ p: { xs: 2.5, sm: 3.5 } }}>
              <ReportForm signedIn />
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, lg: 5 }}>
          <Card>
            <CardContent sx={{ p: { xs: 2.5, sm: 3.5 } }}>
              <Typography variant="h6" component="h2" sx={{ mb: 2 }}>Consultar estado</Typography>
              <ReportTracker signedIn />
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </>
  );
}
