wonum in (Select top 10 wonum from workorder 
Where ( (datediff(dd,0,sneconstraint) <= datediff(dd,0,dateadd(day,7,getdate())))
      Or (datediff(dd,0,schedstart) <= datediff(dd,0,dateadd(day,7,getdate())))
      )
And Status in ('DISPATCH','TRAVEL','ONSITE','START','ONHOLD','WOCOMP')
And woclass='WORKORDER' and worktype='PPM'
And wonum in (Select wonum from assignment where laborcode in (Select laborcode from labor where status = 'ASSIGNED' and personid=:personid and IRV_CONTMETHOD='MOBILE')))