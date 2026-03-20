from datetime import datetime
from fastapi import APIRouter, HTTPException
from api.models.kundali import KundaliRequest, KundaliResponse, VargaChartData
from api.services.kundali_service import calc_kundali
from api.services.divisional_charts import calc_varga_chart, VARGA_NAMES

router = APIRouter()


@router.post("/kundali", response_model=KundaliResponse)
def calculate_kundali(req: KundaliRequest):
    try:
        dt = datetime.strptime(f"{req.date} {req.time}", "%Y-%m-%d %H:%M")
    except ValueError as e:
        raise HTTPException(status_code=422, detail=f"Invalid date/time: {e}")

    data, err = calc_kundali(
        year=dt.year, month=dt.month, day=dt.day,
        hour=dt.hour, minute=dt.minute,
        lat=req.lat, lon=req.lon, tz=req.tz,
        name=req.name, place=req.place,
        ayanamsha_mode=req.ayanamsha,
        rahu_mode=req.rahu_mode,
        calc_options=req.calc_options,
    )
    if err:
        raise HTTPException(status_code=500, detail=err)
    return data


@router.get("/kundali/vargas")
def list_vargas():
    """List all available divisional charts."""
    return [
        {"division": k, "code": v[0], "name": v[1], "signification": v[2]}
        for k, v in sorted(VARGA_NAMES.items())
    ]
