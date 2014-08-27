datediff(dd,0,schedstart) <= datediff(dd,0,dateadd(day,2,getdate()))
And Status in ('DISPATCH','TRAVEL','ONSITE','START','ONHOLD','WOCOMP')
And woclass='WORKORDER' And worktype='RW'
And wonum in (Select wonum from assignment where status = 'ASSIGNED' and laborcode in (Select laborcode from labor where personid=:personid and IRV_CONTMETHOD='MOBILE'))